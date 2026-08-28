package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.service.ExpenseCategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/expense-categories")
@RequiredArgsConstructor
@Tag(name = "Expense Categories", description = "Manage expense categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    @PostMapping
    @Operation(summary = "Create a new expense category")
    public ResponseEntity<ExpenseCategoryResponse> createCategory(
            @Valid @RequestBody CreateExpenseCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseCategoryService.createCategory(request));
    }

    @GetMapping
    @Operation(summary = "Get all expense categories for current owner")
    public ResponseEntity<List<ExpenseCategoryResponse>> getMyCategories() {
        return ResponseEntity.ok(expenseCategoryService.getMyCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get expense category by ID")
    public ResponseEntity<ExpenseCategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseCategoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update expense category")
    public ResponseEntity<ExpenseCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseCategoryRequest request) {
        return ResponseEntity.ok(expenseCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete expense category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        expenseCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
