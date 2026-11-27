package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.spring_boot_project_api.enums.Furnishing;
import com.example.spring_boot_project_api.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

        @Size(max = 100, message = "House number must not exceed 100 characters")
        String houseNumber,

        @Size(max = 100, message = "Street number must not exceed 100 characters")
        String streetNumber,

        @Size(max = 150, message = "Sangkat must not exceed 150 characters")
        String sangkat,

        @Size(max = 150, message = "Khan must not exceed 150 characters")
        String khan,

        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        String postalCode,

        BigDecimal latitude,

        BigDecimal longitude,

        @Size(max = 100, message = "City/Province must not exceed 100 characters")
        String cityProvince,

        @Size(max = 1000, message = "Google Maps URL must not exceed 1000 characters")
        String googleMapsUrl,

        Integer totalRooms,

        Integer availableRooms,

        String description,

        PropertyType propertyType,

        Furnishing furnishing,

        LocalDate availableFrom,

        @PositiveOrZero(message = "Advance payment cannot be negative")
        BigDecimal advancePayment,

        @Positive(message = "Bedrooms must be greater than 0")
        Integer bedrooms,

        @Positive(message = "Bathrooms must be greater than 0")
        Integer bathrooms,

        @Positive(message = "Area must be greater than 0")
        BigDecimal areaSqm,

        @Positive(message = "Land size must be greater than 0")
        BigDecimal landSizeSqm,

        @Positive(message = "Number of floors must be greater than 0")
        Integer numberOfFloors
) {
    public CreateHouseRequest(String name, String slug, String code, String address,
            String postalCode, BigDecimal latitude, BigDecimal longitude, String cityProvince,
            String googleMapsUrl, Integer totalRooms, Integer availableRooms, String description,
            PropertyType propertyType, Furnishing furnishing, LocalDate availableFrom,
            BigDecimal advancePayment, Integer bedrooms, Integer bathrooms, BigDecimal areaSqm,
            BigDecimal landSizeSqm, Integer numberOfFloors) {
        this(name, slug, code, address, null, null, null, null, postalCode, latitude, longitude,
                cityProvince, googleMapsUrl, totalRooms, availableRooms, description, propertyType,
                furnishing, availableFrom, advancePayment, bedrooms, bathrooms, areaSqm, landSizeSqm,
                numberOfFloors);
    }
}
