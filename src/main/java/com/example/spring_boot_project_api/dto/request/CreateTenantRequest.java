package com.example.spring_boot_project_api.dto.request;

import java.time.LocalDate;

import com.example.spring_boot_project_api.model.Gender;
import com.example.spring_boot_project_api.model.OccupationType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(

        // Optional. When provided, links this tenant profile to an existing user
        // login account. When null, a tenant profile is created without a user account.
        Long userId,

        @NotBlank(message = "Tenant code is required")
        @Size(max = 50, message = "Tenant code must not exceed 50 characters")
        String tenantCode,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        Gender gender,

        LocalDate dateOfBirth,

        @NotBlank(message = "Phone is required")
        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        OccupationType occupationType,

        @Size(max = 150, message = "Occupation must not exceed 150 characters")
        String occupation,

        @Size(max = 200, message = "School or company must not exceed 200 characters")
        String schoolOrCompany,

        @Size(max = 50, message = "Id type must not exceed 50 characters")
        String idType,

        @Size(max = 100, message = "Id number must not exceed 100 characters")
        String idNumber,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 100, message = "House number must not exceed 100 characters")
        String houseNumber,

        @Size(max = 100, message = "Street number must not exceed 100 characters")
        String streetNumber,

        @Size(max = 150, message = "Sangkat must not exceed 150 characters")
        String sangkat,

        @Size(max = 150, message = "Khan must not exceed 150 characters")
        String khan,

        @Size(max = 150, message = "City/Province must not exceed 150 characters")
        String cityProvince
) {
    public CreateTenantRequest(Long userId, String tenantCode, String firstName, String lastName,
            Gender gender, LocalDate dateOfBirth, String phone, String email,
            OccupationType occupationType, String occupation, String schoolOrCompany,
            String idType, String idNumber, String address) {
        this(userId, tenantCode, firstName, lastName, gender, dateOfBirth, phone, email,
                occupationType, occupation, schoolOrCompany, idType, idNumber, address,
                null, null, null, null, null);
    }
}
