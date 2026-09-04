package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.mapper.ExpenseMapper;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ExpenseService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.TransactionScoped;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    // private final PaymentMethodRepository paymentMethodRepository;
    private final ExpenseMapper mapper;

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Long ownerId = request.ownerId();

        // 1. Validate Owner
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found with ID: " + ownerId));

        // 2. Validate Duplicate Expense No for this Owner
        if (expenseRepository.existsByOwnerIdAndExpenseNo(ownerId, request.expenseNo())) {
            throw new IllegalArgumentException("Expense number already exists for this owner: " + request.expenseNo());
        }

        // 3. Enforce Cross-Owner Isolation for Category
        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Category ID " + request.categoryId() + " does not belong to owner ID " + ownerId));

        // 4. Enforce Cross-Owner Isolation for House (Optional)
        House house = null;
        if (request.houseId() != null) {
            house = houseRepository.findByIdAndOwnerId(request.houseId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("House ID " + request.houseId() + " does not belong to owner ID " + ownerId));
        }

        // 5. Enforce Cross-Owner Isolation for Payment Method (Optional)
        // PaymentMethod paymentMethod = null;
        // if (request.paymentMethodId() != null) {
        //     paymentMethod = paymentMethodRepository.findByIdAndOwnerId(request.paymentMethodId(), ownerId)
        //             .orElseThrow(() -> new IllegalArgumentException("Payment Method ID " + request.paymentMethodId() + " does not belong to owner ID " + ownerId));
        // }

        // 6. Creator User (Optional)
        User createdBy = null;
        if (request.createdById() != null) {
            createdBy = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new EntityNotFoundException("Creator user not found with ID: " + request.createdById()));
        }

        Expense entity = mapper.toEntity(request, owner, house, category,null, createdBy);
        Expense savedEntity = expenseRepository.save(entity);
        return mapper.toResponse(savedEntity);
    }

    @Override
    public ExpenseResponse getByIdAndOwnerId(Long id, Long ownerId) {
        Expense entity = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));
        return mapper.toResponse(entity);
    }

    @Override
    public List<ExpenseResponse> getAllByOwnerId(Long ownerId) {
        return expenseRepository.findByOwnerId(ownerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<ExpenseResponse> getAllByOwnerId(Long ownerId, Pageable pageable) {
        return expenseRepository.findByOwnerId(ownerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Long ownerId = request.ownerId();

        Expense existingExpense = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));

        if (expenseRepository.existsByOwnerIdAndExpenseNoAndIdNot(ownerId, request.expenseNo(), id)) {
            throw new IllegalArgumentException("Expense number already exists for this owner: " + request.expenseNo());
        }

        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(request.categoryId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Category ID " + request.categoryId() + " does not belong to owner ID " + ownerId));

        House house = null;
        if (request.houseId() != null) {
            house = houseRepository.findByIdAndOwnerId(request.houseId(), ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("House ID " + request.houseId() + " does not belong to owner ID " + ownerId));
        }

        // PaymentMethod paymentMethod = null;
        // if (request.paymentMethodId() != null) {
        //     paymentMethod = paymentMethodRepository.findByIdAndOwnerId(request.paymentMethodId(), ownerId)
        //             .orElseThrow(() -> new IllegalArgumentException("Payment Method ID " + request.paymentMethodId() + " does not belong to owner ID " + ownerId));
        // }

        existingExpense.setExpenseNo(request.expenseNo());
        existingExpense.setHouse(house);
        existingExpense.setCategory(category);
        existingExpense.setDescription(request.description());
        existingExpense.setAmount(request.amount());
        existingExpense.setExpenseDate(request.expenseDate());
        // existingExpense.setPaymentMethod(paymentMethod);
        if (request.status() != null) {
            existingExpense.setStatus(request.status());
        }

        Expense updatedEntity = expenseRepository.save(existingExpense);
        return mapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        Expense expense = expenseRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found with ID: " + id + " for owner: " + ownerId));
        expenseRepository.delete(expense);
    }
}
