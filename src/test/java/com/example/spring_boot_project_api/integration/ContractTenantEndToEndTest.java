package com.example.spring_boot_project_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.enums.RoomTypeStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;

@SpringBootTest(properties = "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
class ContractTenantEndToEndTest {

    @Autowired private ContractTenantService contractTenantService;
    @Autowired private UserRepository userRepository;
    @Autowired private HouseRepository houseRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RentalContractRepository rentalContractRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ContractTenantRepository contractTenantRepository;

    private User owner;
    private User tenantUser;
    private House house;
    private RoomType roomType;
    private Room room;
    private RentalContract contract;
    private Tenant tenant;
    private Long contractTenantId;

    private final List<Long> extraContractTenantIds = new ArrayList<>();
    private final List<Long> extraTenantIds = new ArrayList<>();
    private final List<Long> extraContractIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Long id : extraContractTenantIds) {
            contractTenantRepository.findById(id).ifPresent(contractTenantRepository::delete);
        }
        if (contractTenantId != null) {
            contractTenantRepository.findById(contractTenantId).ifPresent(contractTenantRepository::delete);
        }
        for (Long id : extraTenantIds) {
            tenantRepository.findById(id).ifPresent(tenantRepository::delete);
        }
        if (tenant != null) {
            tenantRepository.findById(tenant.getId()).ifPresent(tenantRepository::delete);
        }
        for (Long id : extraContractIds) {
            rentalContractRepository.findById(id).ifPresent(rentalContractRepository::delete);
        }
        if (contract != null) {
            rentalContractRepository.findById(contract.getId()).ifPresent(rentalContractRepository::delete);
        }
        if (room != null) {
            roomRepository.findById(room.getId()).ifPresent(roomRepository::delete);
        }
        if (roomType != null) {
            roomTypeRepository.findById(roomType.getId()).ifPresent(roomTypeRepository::delete);
        }
        if (house != null) {
            houseRepository.findById(house.getId()).ifPresent(houseRepository::delete);
        }
        if (tenantUser != null) {
            userRepository.findById(tenantUser.getId()).ifPresent(userRepository::delete);
        }
        if (owner != null) {
            userRepository.findById(owner.getId()).ifPresent(userRepository::delete);
        }
    }

    @Test
    void addTenantToContract_tenantWithoutLinkedUser() {
        setupFixtures(false);

        ContractTenantResponse response = contractTenantService.addTenantToContract(
                new ContractTenantRequest(contract.getId(), tenant.getId(), false, LocalDate.now(), null));

        contractTenantId = response.id();
        assertThat(response.tenantId()).isEqualTo(tenant.getId());
    }

    @Test
    void addTenantToContract_tenantWithLinkedUser() {
        setupFixtures(true);

        ContractTenantResponse response = contractTenantService.addTenantToContract(
                new ContractTenantRequest(contract.getId(), tenant.getId(), false, LocalDate.now(), null));

        contractTenantId = response.id();
        assertThat(response.tenantId()).isEqualTo(tenant.getId());
    }

    @Test
    void scenarioA_primaryTenantWithNullAddress_thenSecondaryTenantWithCompleteAddress_partyBRemainsNull() {
        setupFixtures(false);
        RentalContract c = createContractWithSnapshots();

        // 1. Add primary tenant with all 5 address fields null
        Tenant primaryTenant = createTenantWithAddress("pri_null", null, null, null, null, null);
        ContractTenantResponse primaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), primaryTenant.getId(), true, LocalDate.now(), null));
        extraContractTenantIds.add(primaryResp.id());

        RentalContract afterPrimary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterPrimary.getTenantHouseNumber()).isNull();
        assertThat(afterPrimary.getTenantStreetNumber()).isNull();
        assertThat(afterPrimary.getTenantSangkat()).isNull();
        assertThat(afterPrimary.getTenantKhan()).isNull();
        assertThat(afterPrimary.getTenantCityProvince()).isNull();

        // 2. Add secondary tenant with complete address
        Tenant secondaryTenant = createTenantWithAddress("sec_addr", "99B", "Street 271", "Boeng Tumpun", "Mean Chey", "Phnom Penh");
        ContractTenantResponse secondaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), secondaryTenant.getId(), false, LocalDate.now(), null));
        extraContractTenantIds.add(secondaryResp.id());

        // Expected: Party B remains null
        RentalContract afterSecondary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterSecondary.getTenantHouseNumber()).isNull();
        assertThat(afterSecondary.getTenantStreetNumber()).isNull();
        assertThat(afterSecondary.getTenantSangkat()).isNull();
        assertThat(afterSecondary.getTenantKhan()).isNull();
        assertThat(afterSecondary.getTenantCityProvince()).isNull();

        // Scenario E: Owner and House snapshots unchanged
        assertOwnerAndHouseSnapshotsUnchanged(afterSecondary);
    }

    @Test
    void scenarioB_primaryTenantWithAddressA_thenSecondaryTenantWithAddressB_partyBRemainsAddressA() {
        setupFixtures(false);
        RentalContract c = createContractWithSnapshots();

        // 1. Add primary tenant with Address A
        Tenant primaryTenant = createTenantWithAddress("pri_a", "12A", "St. 2004", "Teuk Thla", "Sen Sok", "Phnom Penh");
        ContractTenantResponse primaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), primaryTenant.getId(), true, LocalDate.now(), null));
        extraContractTenantIds.add(primaryResp.id());

        RentalContract afterPrimary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterPrimary.getTenantHouseNumber()).isEqualTo("12A");
        assertThat(afterPrimary.getTenantStreetNumber()).isEqualTo("St. 2004");
        assertThat(afterPrimary.getTenantSangkat()).isEqualTo("Teuk Thla");
        assertThat(afterPrimary.getTenantKhan()).isEqualTo("Sen Sok");
        assertThat(afterPrimary.getTenantCityProvince()).isEqualTo("Phnom Penh");

        // 2. Add secondary tenant with Address B
        Tenant secondaryTenant = createTenantWithAddress("sec_b", "55B", "St. 310", "BKK 1", "BKK", "Siem Reap");
        ContractTenantResponse secondaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), secondaryTenant.getId(), false, LocalDate.now(), null));
        extraContractTenantIds.add(secondaryResp.id());

        // Expected: Party B remains primary tenant's Address A
        RentalContract afterSecondary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterSecondary.getTenantHouseNumber()).isEqualTo("12A");
        assertThat(afterSecondary.getTenantStreetNumber()).isEqualTo("St. 2004");
        assertThat(afterSecondary.getTenantSangkat()).isEqualTo("Teuk Thla");
        assertThat(afterSecondary.getTenantKhan()).isEqualTo("Sen Sok");
        assertThat(afterSecondary.getTenantCityProvince()).isEqualTo("Phnom Penh");

        // Scenario E: Owner and House snapshots unchanged
        assertOwnerAndHouseSnapshotsUnchanged(afterSecondary);
    }

    @Test
    void scenarioC_historicalContractWithNullSnapshots_addSecondaryTenant_partyBRemainsNull() {
        setupFixtures(false);
        RentalContract c = createContractWithSnapshots();

        // Historical contract has null Party B
        assertThat(c.getTenantHouseNumber()).isNull();
        assertThat(c.getTenantCityProvince()).isNull();

        // Add secondary tenant with complete address
        Tenant secondaryTenant = createTenantWithAddress("sec_c", "77C", "St. 110", "Wat Phnom", "Daun Penh", "Phnom Penh");
        ContractTenantResponse secondaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), secondaryTenant.getId(), false, LocalDate.now(), null));
        extraContractTenantIds.add(secondaryResp.id());

        // Expected: Party B remains null
        RentalContract afterSecondary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterSecondary.getTenantHouseNumber()).isNull();
        assertThat(afterSecondary.getTenantStreetNumber()).isNull();
        assertThat(afterSecondary.getTenantSangkat()).isNull();
        assertThat(afterSecondary.getTenantKhan()).isNull();
        assertThat(afterSecondary.getTenantCityProvince()).isNull();

        // Scenario E: Owner and House snapshots unchanged
        assertOwnerAndHouseSnapshotsUnchanged(afterSecondary);
    }

    @Test
    void scenarioD_historicalContract_explicitPrimaryTenantThenSecondaryTenant_cannotOverwrite() {
        setupFixtures(false);
        RentalContract c = createContractWithSnapshots();

        // Historical contract has null Party B
        assertThat(c.getTenantHouseNumber()).isNull();

        // Explicitly assign primary tenant with Address A
        Tenant primaryTenant = createTenantWithAddress("pri_d", "88D", "St. 598", "Chrang Chamreh", "Russey Keo", "Phnom Penh");
        ContractTenantResponse primaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), primaryTenant.getId(), true, LocalDate.now(), null));
        extraContractTenantIds.add(primaryResp.id());

        RentalContract afterPrimary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterPrimary.getTenantHouseNumber()).isEqualTo("88D");
        assertThat(afterPrimary.getTenantStreetNumber()).isEqualTo("St. 598");
        assertThat(afterPrimary.getTenantSangkat()).isEqualTo("Chrang Chamreh");
        assertThat(afterPrimary.getTenantKhan()).isEqualTo("Russey Keo");
        assertThat(afterPrimary.getTenantCityProvince()).isEqualTo("Phnom Penh");

        // Add secondary tenant with Address B
        Tenant secondaryTenant = createTenantWithAddress("sec_d", "99E", "St. 63", "Tonle Bassac", "Chamkarmon", "Battambang");
        ContractTenantResponse secondaryResp = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), secondaryTenant.getId(), false, LocalDate.now(), null));
        extraContractTenantIds.add(secondaryResp.id());

        // Expected: Party B remains primary tenant's Address A
        RentalContract afterSecondary = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertThat(afterSecondary.getTenantHouseNumber()).isEqualTo("88D");
        assertThat(afterSecondary.getTenantStreetNumber()).isEqualTo("St. 598");
        assertThat(afterSecondary.getTenantSangkat()).isEqualTo("Chrang Chamreh");
        assertThat(afterSecondary.getTenantKhan()).isEqualTo("Russey Keo");
        assertThat(afterSecondary.getTenantCityProvince()).isEqualTo("Phnom Penh");

        // Scenario E: Owner and House snapshots unchanged
        assertOwnerAndHouseSnapshotsUnchanged(afterSecondary);
    }

    @Test
    void scenarioE_ownerAndHouseSnapshotsRemainUnchangedAcrossOperations() {
        setupFixtures(false);
        RentalContract c = createContractWithSnapshots();

        Tenant primary = createTenantWithAddress("pri_e", "101", "Main St", "Sangkat 1", "Khan 1", "Phnom Penh");
        ContractTenantResponse resp1 = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), primary.getId(), true, LocalDate.now(), null));
        extraContractTenantIds.add(resp1.id());

        Tenant secondary = createTenantWithAddress("sec_e", "202", "Side St", "Sangkat 2", "Khan 2", "Siem Reap");
        ContractTenantResponse resp2 = contractTenantService.addTenantToContract(
                new ContractTenantRequest(c.getId(), secondary.getId(), false, LocalDate.now(), null));
        extraContractTenantIds.add(resp2.id());

        RentalContract reloaded = rentalContractRepository.findById(c.getId()).orElseThrow();
        assertOwnerAndHouseSnapshotsUnchanged(reloaded);
    }

    private void assertOwnerAndHouseSnapshotsUnchanged(RentalContract c) {
        assertThat(c.getOwnerFullName()).isEqualTo("Repro Owner");
        assertThat(c.getOwnerHouseNumber()).isEqualTo("OWNER-10");
        assertThat(c.getOwnerStreetNumber()).isEqualTo("Owner St");
        assertThat(c.getOwnerSangkat()).isEqualTo("Owner Sangkat");
        assertThat(c.getOwnerKhan()).isEqualTo("Owner Khan");
        assertThat(c.getOwnerCityProvince()).isEqualTo("Phnom Penh");

        assertThat(c.getHouseHouseNumber()).isEqualTo("HOUSE-20");
        assertThat(c.getHouseStreetNumber()).isEqualTo("House St");
        assertThat(c.getHouseSangkat()).isEqualTo("House Sangkat");
        assertThat(c.getHouseKhan()).isEqualTo("House Khan");
        assertThat(c.getHouseCityProvince()).isEqualTo("Phnom Penh");
    }

    private RentalContract createContractWithSnapshots() {
        String token = UUID.randomUUID().toString().substring(0, 8);
        RentalContract c = RentalContract.builder()
                .ownerId(owner.getId())
                .ownerFullName("Repro Owner")
                .ownerHouseNumber("OWNER-10")
                .ownerStreetNumber("Owner St")
                .ownerSangkat("Owner Sangkat")
                .ownerKhan("Owner Khan")
                .ownerCityProvince("Phnom Penh")
                .houseHouseNumber("HOUSE-20")
                .houseStreetNumber("House St")
                .houseSangkat("House Sangkat")
                .houseKhan("House Khan")
                .houseCityProvince("Phnom Penh")
                .contractNo("CT-" + token)
                .roomId(room.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("100.00"))
                .dueDay(5)
                .status(RentalContractStatus.DRAFT)
                .build();
        c = rentalContractRepository.save(c);
        extraContractIds.add(c.getId());
        return c;
    }

    private Tenant createTenantWithAddress(String codeSuffix, String houseNo, String streetNo,
            String sangkat, String khan, String cityProvince) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Tenant t = Tenant.builder()
                .owner(owner)
                .house(house)
                .tenantCode("T-" + token + "-" + codeSuffix)
                .firstName("Tenant" + codeSuffix)
                .lastName("Test")
                .phone("099" + (int)(Math.random() * 900000 + 100000))
                .status(TenantStatus.ACTIVE)
                .houseNumber(houseNo)
                .streetNumber(streetNo)
                .sangkat(sangkat)
                .khan(khan)
                .cityProvince(cityProvince)
                .build();
        t = tenantRepository.save(t);
        extraTenantIds.add(t.getId());
        return t;
    }


    private void setupFixtures(boolean tenantHasUser) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        owner = User.builder()
                .username("repro_owner_" + token)
                .email("repro_owner_" + token + "@example.com")
                .passwordHash("repro")
                .firstName("Repro")
                .lastName("Owner")
                .build();
        owner = userRepository.save(owner);

        house = House.builder()
                .owner(owner)
                .name("Repro House " + token)
                .slug("repro-house-" + token)
                .code("RH" + token)
                .address("Repro Address")
                .build();
        house = houseRepository.save(house);

        roomType = new RoomType();
        roomType.setOwnerId(owner.getId());
        roomType.setName("Repro Type " + token);
        roomType.setCode("RT" + token);
        roomType.setStatus(RoomTypeStatus.ACTIVE);
        roomType = roomTypeRepository.save(roomType);

        room = new Room();
        room.setOwnerId(owner.getId());
        room.setHouseId(house.getId());
        room.setRoomTypeId(roomType.getId());
        room.setRoomNumber("R" + token);
        room.setMonthlyRent(new BigDecimal("100.00"));
        room.setCapacity(1);
        room.setStatus(RoomStatus.AVAILABLE);
        room = roomRepository.save(room);

        contract = RentalContract.builder()
                .ownerId(owner.getId())
                .contractNo("C-" + token)
                .roomId(room.getId())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("100.00"))
                .dueDay(5)
                .status(RentalContractStatus.DRAFT)
                .build();
        contract = rentalContractRepository.save(contract);

        User tenantUser = null;
        if (tenantHasUser) {
            tenantUser = User.builder()
                    .username("repro_tenant_" + token)
                    .email("repro_tenant_" + token + "@example.com")
                    .passwordHash("repro")
                    .firstName("Repro")
                    .lastName("Tenant")
                    .build();
            this.tenantUser = userRepository.save(tenantUser);
        }

        tenant = Tenant.builder()
                .user(tenantUser)
                .owner(owner)
                .house(house)
                .tenantCode("T" + token)
                .firstName("Repro")
                .lastName("Tenant")
                .phone("000-" + token)
                .status(TenantStatus.ACTIVE)
                .build();
        tenant = tenantRepository.save(tenant);

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(owner.getUsername(), "repro", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }
}