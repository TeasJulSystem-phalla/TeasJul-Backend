package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.CreateExpenseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.ExpenseCategoryNotFoundException;
import com.example.spring_boot_project_api.exception.ExpenseNotFoundException;
import com.example.spring_boot_project_api.mapper.ExpenseMapper;
import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.ExpenseStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.ExpenseCategoryRepository;
import com.example.spring_boot_project_api.repository.ExpenseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.ExpenseServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ExpenseMapper expenseMapper = new ExpenseMapper();

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createExpenseShouldReturnCreatedExpense() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        ExpenseCategory category = expenseCategory();
        when(expenseCategoryRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(category));
        when(expenseRepository.existsByOwnerIdAndExpenseNo(1L, "EXP-001")).thenReturn(false);
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> {
                    Expense e = invocation.getArgument(0);
                    e.setId(1L);
                    e.setCreatedAt(LocalDateTime.now());
                    e.setUpdatedAt(LocalDateTime.now());
                    return e;
                });

        CreateExpenseRequest request = new CreateExpenseRequest(
                "EXP-001", null, 1L, "Electric bill", BigDecimal.valueOf(150.00),
                LocalDate.now(), null, "PAID");

        setSecurityContext(principal());

        ExpenseResponse response = expenseService.createExpense(request);

        assertThat(response.expenseNo()).isEqualTo("EXP-001");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(response.status()).isEqualTo(ExpenseStatus.PAID);
    }

    @Test
    void createExpenseShouldThrowWhenDuplicateExpenseNo() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseRepository.existsByOwnerIdAndExpenseNo(1L, "EXP-001")).thenReturn(true);

        CreateExpenseRequest request = new CreateExpenseRequest(
                "EXP-001", null, 1L, "Electric bill", BigDecimal.valueOf(150.00),
                LocalDate.now(), null, "PAID");

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseService.createExpense(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Expense number already exists");
    }

    @Test
    void createExpenseShouldThrowWhenCategoryNotFound() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseRepository.existsByOwnerIdAndExpenseNo(1L, "EXP-001")).thenReturn(false);
        when(expenseCategoryRepository.findByIdAndOwnerId(99L, 1L))
                .thenReturn(Optional.empty());

        CreateExpenseRequest request = new CreateExpenseRequest(
                "EXP-001", null, 99L, "Electric bill", BigDecimal.valueOf(150.00),
                LocalDate.now(), null, "PAID");

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseService.createExpense(request))
                .isInstanceOf(ExpenseCategoryNotFoundException.class);
    }

    @Test
    void getMyExpensesShouldReturnOwnerExpenses() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        Expense expense = expense();
        when(expenseRepository.findAllByOwnerIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(expense));

        setSecurityContext(principal());

        List<ExpenseResponse> results = expenseService.getMyExpenses();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).expenseNo()).isEqualTo("EXP-001");
    }

    @Test
    void getExpenseByIdShouldReturnExpense() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        Expense expense = expense();
        when(expenseRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(expense));

        setSecurityContext(principal());

        ExpenseResponse response = expenseService.getExpenseById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.expenseNo()).isEqualTo("EXP-001");
    }

    @Test
    void getExpenseByIdShouldThrowWhenNotFound() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseRepository.findByIdAndOwnerId(99L, 1L))
                .thenReturn(Optional.empty());

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseService.getExpenseById(99L))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void updateExpenseShouldReturnUpdatedExpense() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        Expense expense = expense();
        when(expenseRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(expense));
        when(expenseRepository.save(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateExpenseRequest request = new UpdateExpenseRequest(
                null, null, null, "Updated description",
                BigDecimal.valueOf(200.00), null, null, "PENDING");

        setSecurityContext(principal());

        ExpenseResponse response = expenseService.updateExpense(1L, request);

        assertThat(response.description()).isEqualTo("Updated description");
        assertThat(response.amount()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(response.status()).isEqualTo(ExpenseStatus.PENDING);
    }

    @Test
    void updateExpenseShouldThrowWhenDuplicateExpenseNo() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        Expense expense = expense();
        when(expenseRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(expense));
        when(expenseRepository.existsByOwnerIdAndExpenseNoAndIdNot(1L, "EXP-002", 1L))
                .thenReturn(true);

        UpdateExpenseRequest request = new UpdateExpenseRequest(
                "EXP-002", null, null, null, null, null, null, null);

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseService.updateExpense(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Expense number already exists");
    }

    @Test
    void deleteExpenseShouldSucceed() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));

        Expense expense = expense();
        when(expenseRepository.findByIdAndOwnerId(1L, 1L))
                .thenReturn(Optional.of(expense));

        setSecurityContext(principal());

        expenseService.deleteExpense(1L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void deleteExpenseShouldThrowWhenNotFound() {
        User owner = user();
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(owner));
        when(expenseRepository.findByIdAndOwnerId(99L, 1L))
                .thenReturn(Optional.empty());

        setSecurityContext(principal());

        assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(ExpenseNotFoundException.class);
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

    private Expense expense() {
        return Expense.builder()
                .id(1L)
                .owner(user())
                .expenseNo("EXP-001")
                .category(expenseCategory())
                .description("Electric bill")
                .amount(BigDecimal.valueOf(150.00))
                .expenseDate(LocalDate.now())
                .status(ExpenseStatus.PAID)
                .createdBy(1L)
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
