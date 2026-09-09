package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.InvalidEmailException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.exception.UserRoleNotFoundException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.service.UserRoleService;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            requireOwner();
        }
        getUserOrThrow(userId);

        return userRoleRepository.findByUserId(userId).stream()
                .map(assignment -> roleMapper.toResponse(assignment.getRole()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getCurrentUserRoles() {
        return userRoleRepository.findByUserId(securityUtils.getCurrentUserId()).stream()
                .map(assignment -> roleMapper.toResponse(assignment.getRole()))
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse assignRoleToUser(Long userId, Long roleId) {
        requireOwner();
        User user = getUserOrThrow(userId);
        Role role = getRoleOrThrow(roleId);

        if (EmailValidator.isInternalRole(role.getCode())
                && !EmailValidator.isOnInternalDomain(user.getEmail())) {
            throw new InvalidEmailException(
                    EmailValidator.internalEmailRequirement());
        }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new DuplicateResourceException("Role " + role.getCode()
                    + " is already assigned to user " + user.getUsername());
        }

        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(userId, roleId))
                .user(user)
                .role(role)
                .build());

        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        requireOwner();
        getUserOrThrow(userId);

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw UserRoleNotFoundException.forIds(userId, roleId);
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.forId(userId));
    }

    private Role getRoleOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> RoleNotFoundException.forId(roleId));
    }
}
