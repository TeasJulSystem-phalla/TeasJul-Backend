package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.model.Expense;
import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.ExpenseStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class ExpenseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void findAllByOwnerIdShouldReturnOwnerExpenses() {
        User owner = persistUser("user1", "user1@example.com");
        ExpenseCategory category = persistCategory(owner, "Electricity", "ELEC");
        persistExpense(owner, category, "EXP-001", BigDecimal.valueOf(150.00));
        persistExpense(owner, category, "EXP-002", BigDecimal.valueOf(200.00));

        var results = expenseRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId());

        assertThat(results).hasSize(2);
    }

    @Test
    void findAllByOwnerIdShouldNotReturnOtherOwnerExpenses() {
        User owner1 = persistUser("user1", "user1@example.com");
        User owner2 = persistUser("user2", "user2@example.com");
        ExpenseCategory category1 = persistCategory(owner1, "Electricity", "ELEC");
        ExpenseCategory category2 = persistCategory(owner2, "Water", "WATER");
        persistExpense(owner1, category1, "EXP-001", BigDecimal.valueOf(150.00));
        persistExpense(owner2, category2, "EXP-002", BigDecimal.valueOf(200.00));

        var results = expenseRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner1.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExpenseNo()).isEqualTo("EXP-001");
    }

    @Test
    void findByIdAndOwnerIdShouldReturnExpense() {
        User owner = persistUser("user1", "user1@example.com");
        ExpenseCategory category = persistCategory(owner, "Electricity", "ELEC");
        Expense expense = persistExpense(owner, category, "EXP-001", BigDecimal.valueOf(150.00));

        Optional<Expense> found = expenseRepository.findByIdAndOwnerId(
                expense.getId(), owner.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getExpenseNo()).isEqualTo("EXP-001");
    }

    @Test
    void findByIdAndOwnerIdShouldReturnEmptyWhenWrongOwner() {
        User owner1 = persistUser("user1", "user1@example.com");
        User owner2 = persistUser("user2", "user2@example.com");
        ExpenseCategory category1 = persistCategory(owner1, "Electricity", "ELEC");
        Expense expense = persistExpense(owner1, category1, "EXP-001", BigDecimal.valueOf(150.00));

        Optional<Expense> found = expenseRepository.findByIdAndOwnerId(
                expense.getId(), owner2.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByOwnerIdAndExpenseNoShouldReturnTrueWhenExists() {
        User owner = persistUser("user1", "user1@example.com");
        ExpenseCategory category = persistCategory(owner, "Electricity", "ELEC");
        persistExpense(owner, category, "EXP-001", BigDecimal.valueOf(150.00));

        boolean exists = expenseRepository.existsByOwnerIdAndExpenseNo(owner.getId(), "EXP-001");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByOwnerIdAndExpenseNoShouldReturnFalseWhenNotExists() {
        User owner = persistUser("user1", "user1@example.com");

        boolean exists = expenseRepository.existsByOwnerIdAndExpenseNo(owner.getId(), "EXP-001");

        assertThat(exists).isFalse();
    }

    private User persistUser(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(user);
    }

    private ExpenseCategory persistCategory(User owner, String name, String code) {
        ExpenseCategory category = ExpenseCategory.builder()
                .owner(owner)
                .name(name)
                .code(code)
                .description(name + " bills")
                .status(ExpenseCategoryStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(category);
    }

    private Expense persistExpense(User owner, ExpenseCategory category, String expenseNo, BigDecimal amount) {
        Expense expense = Expense.builder()
                .owner(owner)
                .expenseNo(expenseNo)
                .category(category)
                .description("Test expense")
                .amount(amount)
                .expenseDate(LocalDate.now())
                .status(ExpenseStatus.PAID)
                .createdBy(owner.getId())
                .build();
        return entityManager.persistFlushFind(expense);
    }
}
