package com.example.spring_boot_project_api.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves the currently authenticated user from the Spring Security context
 * (populated by the JWT filter). Ownership for owner-scoped resources must be
 * derived from this user, never from the client request.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_TENANT = "TENANT";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /** Whether the current user has the given role code (role codes are case-sensitive). */
    public boolean hasRole(String roleCode) {
        return userRoleRepository.existsByUserIdAndRoleCode(getCurrentUserId(), roleCode);
    }

    /** Whether the given user has the given role code. */
    public boolean hasRole(Long userId, String roleCode) {
        return userRoleRepository.existsByUserIdAndRoleCode(userId, roleCode);
    }
}
