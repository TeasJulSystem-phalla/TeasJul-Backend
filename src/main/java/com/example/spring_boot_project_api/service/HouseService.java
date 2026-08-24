package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;

public interface HouseService {

    HouseResponse createHouse(CreateHouseRequest request);

    HouseResponse getHouseById(Long id);

    List<HouseResponse> getMyHouses();

    HouseResponse updateHouse(Long id, UpdateHouseRequest request);

    void deleteHouse(Long id);
}
