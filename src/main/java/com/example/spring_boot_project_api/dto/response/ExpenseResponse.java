package com.example.spring_boot_project_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.spring_boot_project_api.model.ExpenseStatus;

public record ExpenseResponse(
        Long id,
        Long ownerId,
        String expenseNo,
        Long houseId,
        Long categoryId,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        Long paymentMethodId,
        ExpenseStatus status,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
