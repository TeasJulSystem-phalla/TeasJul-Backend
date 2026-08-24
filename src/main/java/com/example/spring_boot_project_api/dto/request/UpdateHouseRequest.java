package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.model.HouseStatus;

import jakarta.validation.constraints.Size;

public record UpdateHouseRequest(

        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @Size(max = 200, message = "Slug must not exceed 200 characters")
        String slug,

        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        BigDecimal latitude,

        BigDecimal longitude,

        Integer totalRooms,

        Integer availableRooms,

        String description,

        HouseStatus status
) {
}
