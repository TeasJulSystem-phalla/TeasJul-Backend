package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.HouseStatus;

public record HouseResponse(
        Long id,
        Long ownerId,
        String name,
        String slug,
        String code,
        String address,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer totalRooms,
        Integer availableRooms,
        String description,
        HouseStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
