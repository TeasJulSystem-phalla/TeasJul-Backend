package com.example.spring_boot_project_api.exception;

public class ExpenseCategoryNotFoundException extends RuntimeException {

    public ExpenseCategoryNotFoundException(String message) {
        super(message);
    }

    public static ExpenseCategoryNotFoundException forId(Long id) {
        return new ExpenseCategoryNotFoundException("Expense category not found with id: " + id);
    }
}
