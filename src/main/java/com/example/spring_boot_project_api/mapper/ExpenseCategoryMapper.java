package com.example.spring_boot_project_api.mapper;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.User;

@Component
public class ExpenseCategoryMapper {

    public ExpenseCategoryResponse toResponse(ExpenseCategory entity) {
        return new ExpenseCategoryResponse(
                entity.getId(),
                entity.getOwner().getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ExpenseCategory toEntity(CreateExpenseCategoryRequest request, User owner) {
        ExpenseCategoryStatus status;
        try {
            status = ExpenseCategoryStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            status = ExpenseCategoryStatus.ACTIVE;
        }

        return ExpenseCategory.builder()
                .owner(owner)
                .name(request.name())
                .code(request.code())
                .description(request.description())
                .status(status)
                .build();
    }
}
