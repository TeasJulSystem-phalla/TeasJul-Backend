package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateExpenseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.ExpenseCategoryNotFoundException;
import com.example.spring_boot_project_api.exception.ExpenseNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.ExpenseMapper;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ExpenseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }

    @Override
    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        User owner = getCurrentOwner();
        Long ownerId = owner.getId();

        if (expenseRepository.existsByOwnerIdAndExpenseNo(ownerId, request.expenseNo())) {
            throw new DuplicateResourceException("Expense number already exists for this owner: " + request.expenseNo());
        }

        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                .orElseThrow(() -> new ExpenseCategoryNotFoundException(
                        "Expense category not found with id: " + request.categoryId()));

        Expense expense = expenseMapper.toEntity(request, owner, category, ownerId);
        Expense saved = expenseRepository.save(expense);
        return expenseMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getMyExpenses() {
        Long ownerId = getCurrentOwner().getId();
        return expenseRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(expenseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = getOwnedExpense(id);
        return expenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, UpdateExpenseRequest request) {
        Expense expense = getOwnedExpense(id);
        Long ownerId = getCurrentOwner().getId();

        if (request.expenseNo() != null && !request.expenseNo().equals(expense.getExpenseNo())) {
            if (expenseRepository.existsByOwnerIdAndExpenseNoAndIdNot(ownerId, request.expenseNo(), id)) {
                throw new DuplicateResourceException(
                        "Expense number already exists for this owner: " + request.expenseNo());
            }
            expense.setExpenseNo(request.expenseNo());
        }

        if (request.categoryId() != null && !request.categoryId().equals(expense.getCategory().getId())) {
            ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                    .orElseThrow(() -> new ExpenseCategoryNotFoundException(
                            "Expense category not found with id: " + request.categoryId()));
            expense.setCategory(category);
        }

        if (request.houseId() != null) {
            expense.setHouseId(request.houseId());
        }

        if (request.description() != null) {
            expense.setDescription(request.description());
        }

        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }

        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }

        if (request.paymentMethodId() != null) {
            expense.setPaymentMethodId(request.paymentMethodId());
        }

        if (request.status() != null) {
            try {
                expense.setStatus(ExpenseStatus.valueOf(request.status()));
            } catch (IllegalArgumentException e) {
                throw new DuplicateResourceException("Invalid expense status: " + request.status());
            }
        }

        Expense saved = expenseRepository.save(expense);
        return expenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = getOwnedExpense(id);
        expenseRepository.delete(expense);
    }

    private Expense getOwnedExpense(Long id) {
        Long currentUserId = getCurrentOwner().getId();
        return expenseRepository.findByIdAndOwnerId(id, currentUserId)
                .orElseThrow(() -> ExpenseNotFoundException.forId(id));
    }
}
