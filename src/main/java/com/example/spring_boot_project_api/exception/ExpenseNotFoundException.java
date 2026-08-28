package com.example.spring_boot_project_api.exception;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(String message) {
        super(message);
    }

    public static ExpenseNotFoundException forId(Long id) {
        return new ExpenseNotFoundException("Expense not found with id: " + id);
    }
}
