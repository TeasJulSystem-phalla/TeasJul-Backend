package com.example.spring_boot_project_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerContractInfoRequest;
import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.HouseService;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.service.TenantService;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * End-to-end integration tests for structured address snapshots.
 *
 * <p>Verifies that:
 * <ol>
 *   <li>Owner, House, and Tenant structured addresses are written to rental_contracts columns.</li>
 *   <li>Contract snapshots are immutable – mutating entities afterwards leaves stored values unchanged.</li>
 *   <li>Historical contracts with null snapshots stay null and are never back-filled.</li>
 *   <li>Partial updates to Tenant and House preserve their structured address.</li>
 * </ol>
 */
@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
@Transactional
class ContractAddressSnapshotEndToEndTest {

    @Autowired private RentalContractService rentalContractService;
    @Autowired private OwnerSettingService ownerSettingService;
    @Autowired private HouseService houseService;
    @Autowired private TenantService tenantService;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseRepository houseRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RentalContractRepository rentalContractRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;

    private User owner;
    private House house;
    private Room room;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        String token = UUID.randomUUID().toString().substring(0, 8);

        owner = userRepository.save(User.builder()
                .username("addr_owner_" + token)
                .email("addr_owner_" + token + "@example.com")
                .passwordHash("secret")
                .firstName("Dara")
                .lastName("Chan")
                .phone("012999888")
                .build());

        // Assign OWNER role (must exist in DB)
        Role ownerRole = roleRepository.findByCode(SecurityUtils.ROLE_OWNER).orElseThrow();
        userRoleRepository.save(new UserRole(
                new UserRoleId(owner.getId(), ownerRole.getId()),
                owner, ownerRole, LocalDateTime.now()));

        // Authenticate as this owner
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        owner.getUsername(), "secret", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));

        // House with structured address
        house = houseRepository.save(House.builder()
                .owner(owner)
                .name("Snap Villa " + token)
                .slug("snap-villa-" + token)
                .code("SV" + token)
                .address("Legacy House Address")
                .cityProvince("Phnom Penh")
                .houseNumber("128A")
                .streetNumber("Street 271")
                .sangkat("Boeng Tumpun")
                .khan("Mean Chey")
                .build());

        RoomType roomType = new RoomType();
        roomType.setOwnerId(owner.getId());
        roomType.setName("Studio " + token);
        roomType.setCode("ST" + token);
        roomType.setStatus(RoomTypeStatus.ACTIVE);
        roomType = roomTypeRepository.save(roomType);

        room = new Room();
        room.setOwnerId(owner.getId());
        room.setHouseId(house.getId());
        room.setRoomTypeId(roomType.getId());
        room.setRoomNumber("R-101");
        room.setStatus(RoomStatus.AVAILABLE);
        room.setMonthlyRent(new BigDecimal("150.00"));
        room.setSecurityDeposit(new BigDecimal("150.00"));
        room.setCapacity(2);
        room = roomRepository.save(room);

        // Tenant with structured address
        tenant = tenantRepository.save(Tenant.builder()
                .owner(owner)
                .tenantCode("TN-" + token)
                .firstName("Sophea")
                .lastName("Meas")
                .phone("010777666")
                .address("Legacy Tenant Address")
                .houseNumber("15")
                .streetNumber("Street 360")
                .sangkat("Boeung Keng Kang 3")
                .khan("Boeung Keng Kang")
                .cityProvince("Phnom Penh")
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UpdateOwnerSettingsRequest settingsWith(UpdateOwnerContractInfoRequest party) {
        return new UpdateOwnerSettingsRequest(
                "USD", "USD", new BigDecimal("4100"), "DD/MM/YYYY", "Asia/Phnom_Penh",
                "Business", "", "", "", "", "#3b82f6", "#10b981", "KM",
                "INV", 10, "", true, true, true, true,
                "គ្រូ", "", "", "", "", "KM", party,
                "RCP", "KM", "", true, true, true, true, "", "");
    }

    private RentalContractRequest contractRequest(Long roomId, Long tenantId) {
        RentalContractRequest r = new RentalContractRequest();
        r.setRoomId(roomId);
        r.setTenantId(tenantId);
        r.setStartDate(LocalDate.now());
        r.setEndDate(LocalDate.now().plusMonths(12));
        r.setMonthlyRent(new BigDecimal("150.00"));
        r.setSecurityDeposit(new BigDecimal("150.00"));
        r.setDueDay(5);
        r.setStatus(RentalContractStatus.ACTIVE);
        return r;
    }

    @Test
    void createContract_capturesStructuredAddressSnapshotsForAllParties() {
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "Dara Chan", Gender.MALE, LocalDate.of(1982, 3, 15), "Cambodian",
                "098765432", "Owner Legacy Address",
                "99", "Preah Sihanouk Blvd", "Chakto Mukh", "Doun Penh", "Phnom Penh",
                "012999888", "Business Owner")));

        RentalContractResponse res = rentalContractService.createContract(
                contractRequest(room.getId(), tenant.getId()));

        RentalContract stored = rentalContractRepository.findById(res.getId()).orElseThrow();

        // Party A (Owner)
        assertThat(stored.getOwnerHouseNumber()).isEqualTo("99");
        assertThat(stored.getOwnerStreetNumber()).isEqualTo("Preah Sihanouk Blvd");
        assertThat(stored.getOwnerSangkat()).isEqualTo("Chakto Mukh");
        assertThat(stored.getOwnerKhan()).isEqualTo("Doun Penh");
        assertThat(stored.getOwnerCityProvince()).isEqualTo("Phnom Penh");

        // Article 1 (House)
        assertThat(stored.getHouseHouseNumber()).isEqualTo("128A");
        assertThat(stored.getHouseStreetNumber()).isEqualTo("Street 271");
        assertThat(stored.getHouseSangkat()).isEqualTo("Boeng Tumpun");
        assertThat(stored.getHouseKhan()).isEqualTo("Mean Chey");
        assertThat(stored.getHouseCityProvince()).isEqualTo("Phnom Penh");

        // Party B (Tenant)
        assertThat(stored.getTenantHouseNumber()).isEqualTo("15");
        assertThat(stored.getTenantStreetNumber()).isEqualTo("Street 360");
        assertThat(stored.getTenantSangkat()).isEqualTo("Boeung Keng Kang 3");
        assertThat(stored.getTenantKhan()).isEqualTo("Boeung Keng Kang");
        assertThat(stored.getTenantCityProvince()).isEqualTo("Phnom Penh");

        // Response fields
        assertThat(res.getOwnerHouseNumber()).isEqualTo("99");
        assertThat(res.getHouseHouseNumber()).isEqualTo("128A");
        assertThat(res.getTenantHouseNumber()).isEqualTo("15");
    }

    @Test
    void existingContractSnapshotsAreImmutableWhenEntitiesChange() {
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "Dara Chan", Gender.MALE, LocalDate.of(1982, 3, 15), "Cambodian",
                "098765432", "Owner Legacy Address",
                "99", "Preah Sihanouk Blvd", "Chakto Mukh", "Doun Penh", "Phnom Penh",
                "012999888", "Business Owner")));

        RentalContractResponse created = rentalContractService.createContract(
                contractRequest(room.getId(), tenant.getId()));
        Long contractId = created.getId();

        // Mutate all three sources
        ownerSettingService.updateSettings(settingsWith(new UpdateOwnerContractInfoRequest(
                "Dara Chan", Gender.MALE, LocalDate.of(1982, 3, 15), "Cambodian",
                "098765432", "Owner New Address",
                "NEW-O-1", "NEW-O-ST", "NEW-O-SANG", "NEW-O-KH", "Siem Reap",
                "012999888", "Business Owner")));

        house.setHouseNumber("NEW-H-1");
        house.setCityProvince("Battambang");
        houseRepository.save(house);

        tenant.setHouseNumber("NEW-T-1");
        tenant.setCityProvince("Kampot");
        tenantRepository.save(tenant);

        // Re-read – snapshots must be unchanged
        RentalContract stored = rentalContractRepository.findById(contractId).orElseThrow();
        assertThat(stored.getOwnerHouseNumber()).isEqualTo("99");
        assertThat(stored.getOwnerCityProvince()).isEqualTo("Phnom Penh");
        assertThat(stored.getHouseHouseNumber()).isEqualTo("128A");
        assertThat(stored.getHouseCityProvince()).isEqualTo("Phnom Penh");
        assertThat(stored.getTenantHouseNumber()).isEqualTo("15");
        assertThat(stored.getTenantCityProvince()).isEqualTo("Phnom Penh");
    }

    @Test
    void historicalContractWithNullSnapshotsIsPreservedAsNull() {
        // Simulate a historical contract with no snapshot columns set
        RentalContract historical = rentalContractRepository.save(RentalContract.builder()
                .ownerId(owner.getId())
                .contractNo("CT-HIST-" + UUID.randomUUID().toString().substring(0, 6))
                .roomId(room.getId())
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().minusMonths(6))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("100.00"))
                .dueDay(1)
                .status(RentalContractStatus.EXPIRED)
                .ownerFullName("Legacy Owner")
                .ownerAddress("Old Legacy Address")
                .build());

        RentalContractResponse res = rentalContractService.getContractById(historical.getId());

        // Snapshot fields must remain null – not back-filled from current settings
        assertThat(res.getOwnerHouseNumber()).isNull();
        assertThat(res.getOwnerStreetNumber()).isNull();
        assertThat(res.getHouseHouseNumber()).isNull();
        assertThat(res.getTenantHouseNumber()).isNull();

        // Legacy fields still readable
        assertThat(res.getOwnerFullName()).isEqualTo("Legacy Owner");
        assertThat(res.getOwnerAddress()).isEqualTo("Old Legacy Address");
    }

    @Test
    void tenantPartialUpdate_preservesExistingStructuredAddress() {
        // Create tenant via service to exercise mapper
        CreateTenantRequest create = new CreateTenantRequest(
                null, "TN-PARTIAL-" + UUID.randomUUID().toString().substring(0, 6),
                "Visal", "Chea", null, null, "092111222", null,
                null, null, null, null, null, "Old Address",
                "10A", "St 51", "Boeng Keng Kang 1", "BKK", "Phnom Penh");
        TenantResponse created = tenantService.createTenant(create);

        assertThat(created.houseNumber()).isEqualTo("10A");
        assertThat(created.streetNumber()).isEqualTo("St 51");

        // Partial update: change phone only, structured address not supplied → preserved
        UpdateTenantRequest update = new UpdateTenantRequest(
                "Visal", "Chea", null, null, "092333444", null,
                null, null, null, null, null, null,
                null, null, null, null, null);
        TenantResponse updated = tenantService.updateTenant(created.id(), update);

        assertThat(updated.phone()).isEqualTo("092333444");
        assertThat(updated.houseNumber()).isEqualTo("10A");
        assertThat(updated.streetNumber()).isEqualTo("St 51");
        assertThat(updated.sangkat()).isEqualTo("Boeng Keng Kang 1");
    }

    @Test
    void housePartialUpdate_preservesExistingStructuredAddress() {
        CreateHouseRequest create = new CreateHouseRequest(
                "Test Villa", "test-villa-" + UUID.randomUUID().toString().substring(0, 6),
                "TV" + UUID.randomUUID().toString().substring(0, 4),
                "Legacy Address",
                "55", "St 200", "Phsar Thmey", "Daun Penh",
                null, null, null, "Phnom Penh",
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        HouseResponse created = houseService.createHouse(create);

        assertThat(created.houseNumber()).isEqualTo("55");
        assertThat(created.streetNumber()).isEqualTo("St 200");

        // Partial update: change name only, structured address not supplied → preserved
        UpdateHouseRequest update = new UpdateHouseRequest(
                "Updated Villa Name", null, null, null,  // name, slug, code, address
                null, null, null, null,                   // houseNumber, streetNumber, sangkat, khan
                null, null, null, null,                   // postalCode, latitude, longitude, cityProvince
                null, null, null, null,                   // googleMapsUrl, totalRooms, availableRooms, description
                null, null, null, null,                   // status, propertyType, furnishing, availableFrom
                null, null, null, null,                   // advancePayment, bedrooms, bathrooms, areaSqm
                null, null);                              // landSizeSqm, numberOfFloors
        HouseResponse updated = houseService.updateHouse(created.id(), update);

        assertThat(updated.name()).isEqualTo("Updated Villa Name");
        assertThat(updated.houseNumber()).isEqualTo("55");
        assertThat(updated.streetNumber()).isEqualTo("St 200");
    }
}
