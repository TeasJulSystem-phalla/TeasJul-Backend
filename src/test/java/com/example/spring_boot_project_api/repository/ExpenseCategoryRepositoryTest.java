package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.model.ExpenseCategory;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class ExpenseCategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Test
    void findAllByOwnerIdShouldReturnOwnerCategories() {
        User owner = persistUser("user1", "user1@example.com");
        persistCategory(owner, "Electricity", "ELEC");
        persistCategory(owner, "Water", "WATER");

        var results = expenseCategoryRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId());

        assertThat(results).hasSize(2);
    }

    @Test
    void findAllByOwnerIdShouldNotReturnOtherOwnerCategories() {
        User owner1 = persistUser("user1", "user1@example.com");
        User owner2 = persistUser("user2", "user2@example.com");
        persistCategory(owner1, "Electricity", "ELEC");
        persistCategory(owner2, "Water", "WATER");

        var results = expenseCategoryRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner1.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCode()).isEqualTo("ELEC");
    }

    @Test
    void findByIdAndOwnerIdShouldReturnCategory() {
        User owner = persistUser("user1", "user1@example.com");
        ExpenseCategory category = persistCategory(owner, "Electricity", "ELEC");

        Optional<ExpenseCategory> found = expenseCategoryRepository.findByIdAndOwnerId(
                category.getId(), owner.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electricity");
    }

    @Test
    void findByIdAndOwnerIdShouldReturnEmptyWhenWrongOwner() {
        User owner1 = persistUser("user1", "user1@example.com");
        User owner2 = persistUser("user2", "user2@example.com");
        ExpenseCategory category = persistCategory(owner1, "Electricity", "ELEC");

        Optional<ExpenseCategory> found = expenseCategoryRepository.findByIdAndOwnerId(
                category.getId(), owner2.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByOwnerIdAndCodeShouldReturnTrueWhenExists() {
        User owner = persistUser("user1", "user1@example.com");
        persistCategory(owner, "Electricity", "ELEC");

        boolean exists = expenseCategoryRepository.existsByOwnerIdAndCode(owner.getId(), "ELEC");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByOwnerIdAndCodeShouldReturnFalseWhenNotExists() {
        User owner = persistUser("user1", "user1@example.com");

        boolean exists = expenseCategoryRepository.existsByOwnerIdAndCode(owner.getId(), "ELEC");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByOwnerIdAndNameShouldReturnTrueWhenExists() {
        User owner = persistUser("user1", "user1@example.com");
        persistCategory(owner, "Electricity", "ELEC");

        boolean exists = expenseCategoryRepository.existsByOwnerIdAndName(owner.getId(), "Electricity");

        assertThat(exists).isTrue();
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
}
