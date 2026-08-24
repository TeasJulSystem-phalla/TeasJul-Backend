package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHouseRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 200, message = "Slug must not exceed 200 characters")
        String slug,

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        BigDecimal latitude,

        BigDecimal longitude,

        Integer totalRooms,

        Integer availableRooms,

        String description
) {
}
