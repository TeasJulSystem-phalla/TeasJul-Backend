package com.example.spring_boot_project_api.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Room;

@Component
public class RoomMapper {

    public Room toEntity(RoomRepuest dto) {
        if (dto == null) return null;

        Room room = new Room();
        room.setHouseId(dto.getHouseId());
        room.setRoomTypeId(dto.getRoomTypeId());
        room.setRoomNumber(dto.getRoomNumber());
        room.setName(dto.getName());
        room.setFloorNumber(dto.getFloorNumber());
        room.setMonthlyRent(dto.getMonthlyRent() != null ? dto.getMonthlyRent() : BigDecimal.ZERO);
        room.setSecurityDeposit(dto.getSecurityDeposit() != null ? dto.getSecurityDeposit() : BigDecimal.ZERO);
        room.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 1);
        room.setStatus(dto.getStatus() != null ? dto.getStatus() : RoomStatus.AVAILABLE);
        room.setDescription(dto.getDescription());
        return room;
    }

    public RoomResponse toDto(Room entity) {
        if (entity == null) return null;

        RoomResponse response = new RoomResponse();
        response.setId(entity.getId());
        response.setOwnerId(entity.getOwnerId());
        response.setHouseId(entity.getHouseId());
        response.setRoomTypeId(entity.getRoomTypeId());
        response.setRoomNumber(entity.getRoomNumber());
        response.setName(entity.getName());
        response.setFloorNumber(entity.getFloorNumber());
        response.setMonthlyRent(entity.getMonthlyRent());
        response.setSecurityDeposit(entity.getSecurityDeposit());
        response.setCapacity(entity.getCapacity());
        response.setStatus(entity.getStatus());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
