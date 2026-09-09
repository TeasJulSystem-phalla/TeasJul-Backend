package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.impl.UserRoleServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceImplTest {

    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RoleMapper roleMapper;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private UserRoleServiceImpl userRoleService;

    private User user(String email) {
        return User.builder().id(7L).username("manager001").email(email).build();
    }

    private Role role(String code) {
        return Role.builder().id(2L).code(code).name(code).build();
    }

    private void stubOwner() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
    }

    @Test
    void assignManagerRoleToRentalComEmailIsAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("manager001@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "MANAGER", "MANAGER", "Manager", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("MANAGER");
    }

    @Test
    void assignInternalRoleToExternalEmailIsRejected() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("manager001@gmail.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("MANAGER")));

        assertThatThrownBy(() -> userRoleService.assignRoleToUser(7L, 2L))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessageContaining("@rental.com");

        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignTenantRoleToExternalEmailIsAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("john.doe@gmail.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("TENANT")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "TENANT", "TENANT", "Tenant", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("TENANT");
    }

    @Test
    void assignTenantRoleToRentalComEmailIsAlsoAllowed() {
        stubOwner();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user("john@rental.com")));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role("TENANT")));
        when(userRoleRepository.existsByUserIdAndRoleId(7L, 2L)).thenReturn(false);
        when(roleMapper.toResponse(any(Role.class)))
                .thenReturn(new RoleResponse(2L, "TENANT", "TENANT", "Tenant", false, null, null));

        RoleResponse response = userRoleService.assignRoleToUser(7L, 2L);

        assertThat(response.code()).isEqualTo("TENANT");
    }
}