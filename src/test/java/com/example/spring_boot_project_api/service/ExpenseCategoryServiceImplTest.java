package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.ExpenseCategoryNotFoundException;
import com.example.spring_boot_project_api.mapper.ExpenseCategoryMapper;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.ExpenseCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExpenseCategoryServiceImplTest {

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ExpenseCategoryMapper expenseCategoryMapper = new ExpenseCategoryMapper();

    @InjectMocks
    private ExpenseCategoryServiceImpl expenseCategoryService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCategoryShouldReturnCreatedCategory() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        CreateExpenseCategoryRequest request = new CreateExpenseCategoryRequest(
                "Electricity", "ELEC", "Electric bills", "ACTIVE");

        when(expenseCategoryRepository.existsByOwnerIdAndCode(1L, "ELEC")).thenReturn(false);
        when(expenseCategoryRepository.existsByOwnerIdAndName(1L, "Electricity")).thenReturn(false);
        when(expenseCategoryRepository.save(any(ExpenseCategory.class)))
                .thenAnswer(invocation -> {
                    ExpenseCategory cat = invocation.getArgument(0);
                    cat.setId(1L);
                    cat.setCreatedAt(LocalDateTime.now());
                    cat.setUpdatedAt(LocalDateTime.now());
                    return cat;
                });

        setSecurityContext(principal());

        ExpenseCategoryResponse response = expenseCategoryService.createCategory(request);

        assertThat(response.name()).isEqualTo("Electricity");
        assertThat(response.code()).isEqualTo("ELEC");
        assertThat(response.status()).isEqualTo(ExpenseCategoryStatus.ACTIVE);
    }

    @Test
    void createCategoryShouldThrowWhenDuplicateCode() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseCategoryRepository.existsByOwnerIdAndCode(1L, "ELEC")).thenReturn(true);

        CreateExpenseCategoryRequest request = new CreateExpenseCategoryRequest(
                "Electricity", "ELEC", "Electric bills", "ACTIVE");

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseCategoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category code already exists");
    }

    @Test
    void createCategoryShouldThrowWhenDuplicateName() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseCategoryRepository.existsByOwnerIdAndCode(1L, "ELEC")).thenReturn(false);
        when(expenseCategoryRepository.existsByOwnerIdAndName(1L, "Electricity")).thenReturn(true);

        CreateExpenseCategoryRequest request = new CreateExpenseCategoryRequest(
                "Electricity", "ELEC", "Electric bills", "ACTIVE");

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseCategoryService.createCategory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category name already exists");
    }

    @Test
    void getMyCategoriesShouldReturnOwnerCategories() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        ExpenseCategory cat = expenseCategory();
        when(expenseCategoryRepository.findAllByOwnerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(cat));

        setSecurityContext(principal());

        List<ExpenseCategoryResponse> results = expenseCategoryService.getMyCategories();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Electricity");
    }

    @Test
    void getCategoryByIdShouldReturnCategory() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        ExpenseCategory cat = expenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(cat));

        setSecurityContext(principal());

        ExpenseCategoryResponse response = expenseCategoryService.getCategoryById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electricity");
    }

    @Test
    void getCategoryByIdShouldThrowWhenNotFound() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseCategoryRepository.findByIdAndOwnerId(99L, 1L))
                .thenReturn(Optional.empty());

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseCategoryService.getCategoryById(99L))
                .isInstanceOf(ExpenseCategoryNotFoundException.class);
    }

    @Test
    void updateCategoryShouldReturnUpdatedCategory() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        ExpenseCategory cat = expenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(cat));
        when(expenseCategoryRepository.save(any(ExpenseCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateExpenseCategoryRequest request = new UpdateExpenseCategoryRequest(
                "Water", null, "Water bills", "INACTIVE");

        setSecurityContext(principal());

        ExpenseCategoryResponse response = expenseCategoryService.updateCategory(1L, request);

        assertThat(response.name()).isEqualTo("Water");
        assertThat(response.description()).isEqualTo("Water bills");
        assertThat(response.status()).isEqualTo(ExpenseCategoryStatus.INACTIVE);
    }

    @Test
    void deleteCategoryShouldSucceed() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        ExpenseCategory cat = expenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(cat));

        setSecurityContext(principal());

        expenseCategoryService.deleteCategory(1L);

        verify(expenseCategoryRepository).delete(cat);
    }

    @Test
    void deleteCategoryShouldThrowWhenNotFound() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseCategoryRepository.findByIdAndOwnerId(99L, 1L))
                .thenReturn(Optional.empty());

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseCategoryService.deleteCategory(99L))
                .isInstanceOf(ExpenseCategoryNotFoundException.class);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .passwordHash("$2a$10$hashed")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ExpenseCategory expenseCategory() {
        return ExpenseCategory.builder()
                .id(1L)
                .owner(user())
                .name("Electricity")
                .code("ELEC")
                .description("Electric bills")
                .status(ExpenseCategoryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("johndoe")
                .password("$2a$10$hashed")
                .authorities("ROLE_USER")
                .build();
    }

    private void setSecurityContext(UserDetails userDetails) {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(userDetails, "n/a"));
    }
}
