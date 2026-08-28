package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;

public record ExpenseCategoryResponse(
        Long id,
        Long ownerId,
        String name,
        String code,
        String description,
        ExpenseCategoryStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
