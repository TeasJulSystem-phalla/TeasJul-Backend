package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.dto.response.UserTenantCandidateResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.UserServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .passwordHash("hashed")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UserDetails principal() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("johndoe")
                .password("n/a")
                .roles("USER")
                .build();
    }

    @Test
    void getUserByIdShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        UserResponse response = userService.getUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getUserByIdShouldThrowWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(securityUtils.getCurrentUserId()).thenReturn(99L);

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getCurrentUserShouldReturnAuthenticatedUser() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal(), "n/a"));
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user()));

        UserResponse response = userService.getCurrentUser();

        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void getAllUsersShouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user()));
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void updateUserShouldOnlyOverwriteProvidedFields() {
        User existing = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        UpdateUserRequest request = new UpdateUserRequest("Johnny", null, "+9876543210", null);

        UserResponse response = userService.updateUser(1L, request);

        assertThat(response.firstName()).isEqualTo("Johnny");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.phone()).isEqualTo("+9876543210");
    }

    @Test
    void deleteUserShouldRemoveUser() {
        User existing = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);

        userService.deleteUser(1L);

        org.mockito.ArgumentCaptor<User> captor =
                org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    // ---- Search available for tenant ----

    private User candidate() {
        return User.builder()
                .id(25L)
                .username("darasok")
                .email("dara@gmail.com")
                .passwordHash("hashed")
                .firstName("Dara")
                .lastName("Sok")
                .phone("012345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchAvailableForTenantReturnsPagedProjectsAndTrimsQuery() {
        Page<User> page = new PageImpl<>(List.of(candidate()), PageRequest.of(0, 10), 1);

        when(userRepository.findAvailableForTenant(eq("dara"), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant("  dara  ", PageRequest.of(0, 10));

        assertThat(response.content()).hasSize(1);
        UserTenantCandidateResponse item = response.content().get(0);
        assertThat(item.id()).isEqualTo(25L);
        assertThat(item.firstName()).isEqualTo("Dara");
        assertThat(item.lastName()).isEqualTo("Sok");
        assertThat(item.email()).isEqualTo("dara@gmail.com");
        assertThat(item.phone()).isEqualTo("012345678");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    void searchAvailableForTenantExposesNoSensitiveFields() {
        Page<User> page = new PageImpl<>(List.of(candidate()), PageRequest.of(0, 10), 1);
        when(userRepository.findAvailableForTenant(eq("dara"), any(Pageable.class))).thenReturn(page);

        UserTenantCandidateResponse item =
                userService.searchAvailableForTenant("dara", PageRequest.of(0, 10)).content().get(0);

        assertThat(item.username()).isNotBlank();
        // passwordHash / tokens etc. are simply not part of the DTO
    }

    @Test
    void searchAvailableForTenantCapsPageSize() {
        User one = candidate();
        User two = candidate();
        two.setId(26L);
        Page<User> page = new PageImpl<>(List.of(one, two), PageRequest.of(0, 10), 2);
        when(userRepository.findAvailableForTenant(eq("x"), any(Pageable.class))).thenReturn(page);

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant("x", PageRequest.of(0, 99999));

        assertThat(response.content()).hasSize(2);

        org.mockito.ArgumentCaptor<Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAvailableForTenant(eq("x"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void searchAvailableForTenantEmptyQueryPassedAsNullDoesNotCrash() {
        when(userRepository.findAvailableForTenant(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<UserTenantCandidateResponse> response =
                userService.searchAvailableForTenant(null, PageRequest.of(0, 10));

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }
}
