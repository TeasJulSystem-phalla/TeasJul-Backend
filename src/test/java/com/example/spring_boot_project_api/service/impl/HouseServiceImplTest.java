package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseImageRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HouseServiceImplTest {

    private static final long OWNER_ID = 7L;

    private HouseRepository houseRepository;
    private SecurityUtils securityUtils;
    private HouseMapper houseMapper;
    private HouseServiceImpl houseService;

    @BeforeEach
    void setUp() {
        houseRepository = mock(HouseRepository.class);
        securityUtils = mock(SecurityUtils.class);
        houseMapper = new HouseMapper();

        houseService = new HouseServiceImpl(
                houseRepository,
                mock(HouseImageRepository.class),
                mock(RoomRepository.class),
                mock(RentalContractRepository.class),
                mock(ContractTenantRepository.class),
                mock(TenantRepository.class),
                mock(UserHouseAccessRepository.class),
                houseMapper,
                mock(RoomMapper.class),
                mock(TenantMapper.class),
                mock(RentalContractMapper.class),
                securityUtils);

        when(securityUtils.getCurrentUserId()).thenReturn(OWNER_ID);
        when(securityUtils.getCurrentUser()).thenReturn(owner());
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(houseRepository.save(any(House.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User owner() {
        return User.builder().id(OWNER_ID).username("owner1").build();
    }

    @Test
    void createHousePreservesCityProvinceAndGoogleMapsUrl() {
        CreateHouseRequest request = new CreateHouseRequest(
                "Villa Mondul", "villa-mondul", "H001", "Street 200", "12000",
                new BigDecimal("11.5564"), new BigDecimal("104.9282"),
                "Phnom Penh", "https://www.google.com/maps/place/Phnom+Penh",
                5, 3, "A nice villa", null, null, null, null, null, null, null, null, null);

        HouseResponse response = houseService.createHouse(request);

        assertThat(response.cityProvince()).isEqualTo("Phnom Penh");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Phnom+Penh");
    }

    @Test
    void updateHousePreservesCityProvinceAndGoogleMapsUrl() {
        House house = House.builder().id(1L).owner(owner()).name("Villa Mondul").build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));

        UpdateHouseRequest request = new UpdateHouseRequest(
                null, null, null, null, null, null, null,
                "Siem Reap", "https://www.google.com/maps/place/Siem+Reap",
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        HouseResponse response = houseService.updateHouse(1L, request);

        assertThat(response.cityProvince()).isEqualTo("Siem Reap");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Siem+Reap");
    }

    @Test
    void getHouseByIdReturnsCityProvinceAndGoogleMapsUrl() {
        House house = House.builder()
                .id(1L)
                .owner(owner())
                .name("Villa Mondul")
                .cityProvince("Kampot")
                .googleMapsUrl("https://www.google.com/maps/place/Kampot")
                .build();
        when(houseRepository.findById(1L)).thenReturn(Optional.of(house));
        when(houseRepository.findById(anyLong())).thenReturn(Optional.of(house));

        HouseResponse response = houseService.getHouseById(1L);

        assertThat(response.cityProvince()).isEqualTo("Kampot");
        assertThat(response.googleMapsUrl()).isEqualTo("https://www.google.com/maps/place/Kampot");
    }
}