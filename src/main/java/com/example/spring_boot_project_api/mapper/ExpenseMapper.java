package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseStatus;
import com.example.spring_boot_project_api.model.User;

@Component
public class ExpenseMapper {

    public ExpenseResponse toResponse(Expense entity) {
        return new ExpenseResponse(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getExpenseNo(),
                entity.getHouseId(),
                entity.getCategory().getId(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getExpenseDate(),
                entity.getPaymentMethodId(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public Expense toEntity(CreateExpenseRequest request, User owner, ExpenseCategory category, Long currentUserId) {
        ExpenseStatus status;
        try {
            status = ExpenseStatus.valueOf(request.status());
        } catch (IllegalArgumentException | NullPointerException e) {
            status = ExpenseStatus.PAID;
        }

        return Expense.builder()
                .owner(owner)
                .expenseNo(request.expenseNo())
                .houseId(request.houseId())
                .category(category)
                .description(request.description())
                .amount(request.amount())
                .expenseDate(request.expenseDate())
                .paymentMethodId(request.paymentMethodId())
                .status(status)
                .createdBy(currentUserId)
                .build();
    }
}
