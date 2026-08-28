package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(
        @NotBlank(message = "Expense number is required")
        @Size(max = 50, message = "Expense number must not exceed 50 characters")
        String expenseNo,

        Long houseId,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        Long paymentMethodId,

        String status
) {
}
