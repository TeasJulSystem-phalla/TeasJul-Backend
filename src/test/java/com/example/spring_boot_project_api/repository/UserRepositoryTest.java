package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.clean-disabled=true"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    void findByUsernameShouldReturnUser() {
        persistUser("johndoe", "john@example.com");

        assertThat(userRepository.findByUsername("johndoe"))
                .hasValueSatisfying(user -> {
                    assertThat(user.getEmail()).isEqualTo("john@example.com");
                    assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(user.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void findByEmailShouldReturnUser() {
        persistUser("janedoe", "jane@example.com");

        assertThat(userRepository.findByEmail("jane@example.com"))
                .hasValueSatisfying(user -> assertThat(user.getUsername()).isEqualTo("janedoe"));
    }

    @Test
    void findByUnknownUsernameShouldReturnEmpty() {
        assertThat(userRepository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void existsByUsernameAndEmailShouldReflectPersistedUsers() {
        persistUser("unique_user", "unique@example.com");

        assertThat(userRepository.existsByUsername("unique_user")).isTrue();
        assertThat(userRepository.existsByEmail("unique@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("missing")).isFalse();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }
}
