package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.example.spring_boot_project_api.dto.request.ExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;

import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.mapper.ExpenseCategoryMapper;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ExpenseCategoryService;
import com.example.spring_boot_project_api.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final UserRepository userRepository;
    private final ExpenseCategoryMapper mapper;

    @Override
    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found with ID: " + request.ownerId()));

        if (expenseCategoryRepository.existsByOwnerIdAndCode(request.ownerId(), request.code())) {
            throw new IllegalArgumentException("Category code already exists for this owner: " + request.code());
        }

        if (expenseCategoryRepository.existsByOwnerIdAndName(request.ownerId(), request.name())) {
            throw new IllegalArgumentException("Category name already exists for this owner: " + request.name());
        }

        ExpenseCategory entity = mapper.toEntity(request, owner);
        ExpenseCategory savedEntity = expenseCategoryRepository.save(entity);
        return mapper.toResponse(savedEntity);
    }

    @Override
    public ExpenseCategoryResponse getByIdAndOwnerId(Long id, Long ownerId) {
        ExpenseCategory entity = expenseCategoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id + " for owner: " + ownerId));
        return mapper.toResponse(entity);
    }

    @Override
    public List<ExpenseCategoryResponse> getAllByOwnerId(Long ownerId) {
        return expenseCategoryRepository.findByOwnerId(ownerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<ExpenseCategoryResponse> getAllByOwnerId(Long ownerId, Pageable pageable) {
        return expenseCategoryRepository.findByOwnerId(ownerId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseCategoryResponse update(Long id, ExpenseCategoryRequest request) {
        ExpenseCategory existingCategory = expenseCategoryRepository.findByIdAndOwnerId(id, request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id + " for owner: " + request.ownerId()));

        if (expenseCategoryRepository.existsByOwnerIdAndCodeAndIdNot(request.ownerId(), request.code(), id)) {
            throw new IllegalArgumentException("Category code already exists for this owner: " + request.code());
        }

        if (expenseCategoryRepository.existsByOwnerIdAndNameAndIdNot(request.ownerId(), request.name(), id)) {
            throw new IllegalArgumentException("Category name already exists for this owner: " + request.name());
        }

        existingCategory.setName(request.name());
        existingCategory.setCode(request.code());
        existingCategory.setDescription(request.description());
        if (request.status() != null) {
            existingCategory.setStatus(request.status());
        }

        ExpenseCategory updatedCategory = expenseCategoryRepository.save(existingCategory);
        return mapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void delete(Long id, Long ownerId) {
        ExpenseCategory category = expenseCategoryRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + id + " for owner: " + ownerId));
        expenseCategoryRepository.delete(category);
    }
}
