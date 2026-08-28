package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateExpenseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;

public interface ExpenseService {

    ExpenseResponse createExpense(CreateExpenseRequest request);

    List<ExpenseResponse> getMyExpenses();

    ExpenseResponse getExpenseById(Long id);

    ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request);

    void deleteExpense(Long id);
}
