package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;

public interface ExpenseCategoryService {

    ExpenseCategoryResponse createCategory(CreateExpenseCategoryRequest request);

    List<ExpenseCategoryResponse> getMyCategories();

    ExpenseCategoryResponse getCategoryById(Long id);

    ExpenseCategoryResponse updateCategory(Long id, UpdateExpenseCategoryRequest request);

    void deleteCategory(Long id);
}
