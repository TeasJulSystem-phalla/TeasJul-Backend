package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.response.OwnerContractInfoResponse;
import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.ContractAddressSnapshotHelper;

@ExtendWith(MockitoExtension.class)
class ContractAddressSnapshotHelperTest {

    @Mock
    private OwnerSettingService ownerSettingService;

    @Mock
    private UserRepository userRepository;

    private ContractAddressSnapshotHelper snapshotHelper;

    @BeforeEach
    void setUp() {
        snapshotHelper = new ContractAddressSnapshotHelper(ownerSettingService, userRepository);
    }

    @Test
    void snapshotOwner_populatesBothLegacyAndStructuredAddress() {
        RentalContract contract = new RentalContract();
        OwnerContractInfoResponse info = new OwnerContractInfoResponse(
                "Sokha Pich", Gender.FEMALE, LocalDate.of(1988, 5, 20),
                "Khmer", "012345678", "Building 4, Street 2004",
                "12B", "Street 2004", "Teuk Thla", "Sen Sok", "Phnom Penh",
                "012888999", "Architect"
        );
        when(ownerSettingService.getContractInfo(1L)).thenReturn(info);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        snapshotHelper.snapshotOwner(contract, 1L);

        assertThat(contract.getOwnerFullName()).isEqualTo("Sokha Pich");
        assertThat(contract.getOwnerGender()).isEqualTo("FEMALE");
        assertThat(contract.getOwnerDateOfBirth()).isEqualTo(LocalDate.of(1988, 5, 20));
        assertThat(contract.getOwnerNationality()).isEqualTo("Khmer");
        assertThat(contract.getOwnerNationalId()).isEqualTo("012345678");
        assertThat(contract.getOwnerAddress()).isEqualTo("Building 4, Street 2004");
        assertThat(contract.getOwnerPhone()).isEqualTo("012888999");
        assertThat(contract.getOwnerOccupation()).isEqualTo("Architect");

        assertThat(contract.getOwnerHouseNumber()).isEqualTo("12B");
        assertThat(contract.getOwnerStreetNumber()).isEqualTo("Street 2004");
        assertThat(contract.getOwnerSangkat()).isEqualTo("Teuk Thla");
        assertThat(contract.getOwnerKhan()).isEqualTo("Sen Sok");
        assertThat(contract.getOwnerCityProvince()).isEqualTo("Phnom Penh");
    }

    @Test
    void snapshotOwner_trimsWhitespaceAndConvertsBlanksToNull() {
        RentalContract contract = new RentalContract();
        OwnerContractInfoResponse info = new OwnerContractInfoResponse(
                "Sokha", null, null, "  ", "", "   ",
                "   ", "", "  ", "   ", "  ",
                "012888999", "  "
        );
        when(ownerSettingService.getContractInfo(1L)).thenReturn(info);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        snapshotHelper.snapshotOwner(contract, 1L);

        assertThat(contract.getOwnerFullName()).isEqualTo("Sokha");
        assertThat(contract.getOwnerPhone()).isEqualTo("012888999");
        assertThat(contract.getOwnerGender()).isNull();
        assertThat(contract.getOwnerNationality()).isNull();
        assertThat(contract.getOwnerNationalId()).isNull();
        assertThat(contract.getOwnerAddress()).isNull();
        assertThat(contract.getOwnerOccupation()).isNull();
        assertThat(contract.getOwnerHouseNumber()).isNull();
        assertThat(contract.getOwnerStreetNumber()).isNull();
        assertThat(contract.getOwnerSangkat()).isNull();
        assertThat(contract.getOwnerKhan()).isNull();
        assertThat(contract.getOwnerCityProvince()).isNull();
    }

    @Test
    void snapshotTenant_freezesStructuredAddressOntoPartyB() {
        RentalContract contract = new RentalContract();
        Tenant tenant = Tenant.builder()
                .houseNumber("45A")
                .streetNumber("St. 310")
                .sangkat("Boeung Keng Kang 2")
                .khan("Boeung Keng Kang")
                .cityProvince("Phnom Penh")
                .build();

        snapshotHelper.snapshotTenant(contract, tenant);

        assertThat(contract.getTenantHouseNumber()).isEqualTo("45A");
        assertThat(contract.getTenantStreetNumber()).isEqualTo("St. 310");
        assertThat(contract.getTenantSangkat()).isEqualTo("Boeung Keng Kang 2");
        assertThat(contract.getTenantKhan()).isEqualTo("Boeung Keng Kang");
        assertThat(contract.getTenantCityProvince()).isEqualTo("Phnom Penh");
    }

    @Test
    void snapshotHouse_freezesStructuredAddressOntoArticle1() {
        RentalContract contract = new RentalContract();
        House house = House.builder()
                .houseNumber("88")
                .streetNumber("Russian Blvd")
                .sangkat("Tuek Thla")
                .khan("Sen Sok")
                .cityProvince("Phnom Penh")
                .build();

        snapshotHelper.snapshotHouse(contract, house);

        assertThat(contract.getHouseHouseNumber()).isEqualTo("88");
        assertThat(contract.getHouseStreetNumber()).isEqualTo("Russian Blvd");
        assertThat(contract.getHouseSangkat()).isEqualTo("Tuek Thla");
        assertThat(contract.getHouseKhan()).isEqualTo("Sen Sok");
        assertThat(contract.getHouseCityProvince()).isEqualTo("Phnom Penh");
    }

    @Test
    void snapshotAll_populatesAllThreePartiesIndependentlyWithoutCrossContamination() {
        RentalContract contract = new RentalContract();
        OwnerContractInfoResponse ownerInfo = new OwnerContractInfoResponse(
                "Owner One", null, null, null, null, null,
                "O-1", "O-St", "O-Sang", "O-Kh", "Phnom Penh",
                "011111111", null
        );
        when(ownerSettingService.getContractInfo(10L)).thenReturn(ownerInfo);
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        House house = House.builder()
                .houseNumber("H-1")
                .streetNumber("H-St")
                .sangkat("H-Sang")
                .khan("H-Kh")
                .cityProvince("Siem Reap")
                .build();

        Tenant tenant = Tenant.builder()
                .houseNumber("T-1")
                .streetNumber("T-St")
                .sangkat("T-Sang")
                .khan("T-Kh")
                .cityProvince("Battambang")
                .build();

        snapshotHelper.snapshotAll(contract, 10L, house, tenant);

        // Party A
        assertThat(contract.getOwnerHouseNumber()).isEqualTo("O-1");
        assertThat(contract.getOwnerStreetNumber()).isEqualTo("O-St");
        assertThat(contract.getOwnerSangkat()).isEqualTo("O-Sang");
        assertThat(contract.getOwnerKhan()).isEqualTo("O-Kh");
        assertThat(contract.getOwnerCityProvince()).isEqualTo("Phnom Penh");

        // Article 1
        assertThat(contract.getHouseHouseNumber()).isEqualTo("H-1");
        assertThat(contract.getHouseStreetNumber()).isEqualTo("H-St");
        assertThat(contract.getHouseSangkat()).isEqualTo("H-Sang");
        assertThat(contract.getHouseKhan()).isEqualTo("H-Kh");
        assertThat(contract.getHouseCityProvince()).isEqualTo("Siem Reap");

        // Party B
        assertThat(contract.getTenantHouseNumber()).isEqualTo("T-1");
        assertThat(contract.getTenantStreetNumber()).isEqualTo("T-St");
        assertThat(contract.getTenantSangkat()).isEqualTo("T-Sang");
        assertThat(contract.getTenantKhan()).isEqualTo("T-Kh");
        assertThat(contract.getTenantCityProvince()).isEqualTo("Battambang");
    }

    @Test
    void snapshotAll_handlesNullPartiesGracefully() {
        RentalContract contract = new RentalContract();
        snapshotHelper.snapshotAll(contract, null, null, null);

        assertThat(contract.getOwnerHouseNumber()).isNull();
        assertThat(contract.getHouseHouseNumber()).isNull();
        assertThat(contract.getTenantHouseNumber()).isNull();
    }
}
