package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;

public interface RoomTypeService {

    RoomTypeResponse createRoomType(RoomTypeRequest request);

    RoomTypeResponse getRoomTypeById(Long id);

    List<RoomTypeResponse> getAllRoomTypes();

    List<RoomTypeResponse> getRoomTypesByOwner(Long ownerId);

    RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request);

    void deleteRoomType(Long id);
}
