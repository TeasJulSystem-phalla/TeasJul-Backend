package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.ExpenseCategoryNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.ExpenseCategoryMapper;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ExpenseCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final UserRepository userRepository;
    private final ExpenseCategoryMapper expenseCategoryMapper;

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }

    @Override
    @Transactional
    public ExpenseCategoryResponse createCategory(CreateExpenseCategoryRequest request) {
        User owner = getCurrentOwner();
        Long ownerId = owner.getId();

        if (expenseCategoryRepository.existsByOwnerIdAndCode(ownerId, request.code())) {
            throw new DuplicateResourceException("Category code already exists for this owner: " + request.code());
        }

        if (expenseCategoryRepository.existsByOwnerIdAndName(ownerId, request.name())) {
            throw new DuplicateResourceException("Category name already exists for this owner: " + request.name());
        }

        ExpenseCategory category = expenseCategoryMapper.toEntity(request, owner);
        ExpenseCategory saved = expenseCategoryRepository.save(category);
        return expenseCategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getMyCategories() {
        Long ownerId = getCurrentOwner().getId();
        return expenseCategoryRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(expenseCategoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseCategoryResponse getCategoryById(Long id) {
        ExpenseCategory category = getOwnedCategory(id);
        return expenseCategoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public ExpenseCategoryResponse updateCategory(Long id, UpdateExpenseCategoryRequest request) {
        ExpenseCategory category = getOwnedCategory(id);
        Long ownerId = getCurrentOwner().getId();

        if (request.name() != null && !request.name().equals(category.getName())) {
            if (expenseCategoryRepository.existsByOwnerIdAndNameAndIdNot(ownerId, request.name(), id)) {
                throw new DuplicateResourceException(
                        "Category name already exists for this owner: " + request.name());
            }
            category.setName(request.name());
        }

        if (request.code() != null && !request.code().equals(category.getCode())) {
            if (expenseCategoryRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, request.code(), id)) {
                throw new DuplicateResourceException(
                        "Category code already exists for this owner: " + request.code());
            }
            category.setCode(request.code());
        }

        if (request.description() != null) {
            category.setDescription(request.description());
        }

        if (request.status() != null) {
            try {
                category.setStatus(ExpenseCategoryStatus.valueOf(request.status()));
            } catch (IllegalArgumentException e) {
                throw new DuplicateResourceException("Invalid category status: " + request.status());
            }
        }

        ExpenseCategory saved = expenseCategoryRepository.save(category);
        return expenseCategoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        ExpenseCategory category = getOwnedCategory(id);
        expenseCategoryRepository.delete(category);
    }

    private ExpenseCategory getOwnedCategory(Long id) {
        Long currentUserId = getCurrentOwner().getId();
        return expenseCategoryRepository.findByIdAndOwnerId(id, currentUserId)
                .orElseThrow(() -> ExpenseCategoryNotFoundException.forId(id));
    }
}
