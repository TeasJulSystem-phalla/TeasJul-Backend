package com.example.spring_boot_project_api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateExpenseRequest(
        String expenseNo,

        Long houseId,

        Long categoryId,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        LocalDate expenseDate,

        Long paymentMethodId,

        String status
) {
}
