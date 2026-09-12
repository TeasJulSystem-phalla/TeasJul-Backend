package com.example.spring_boot_project_api.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.dto.response.UserSummaryResponse;
import com.example.spring_boot_project_api.dto.response.UserTenantCandidateResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.PermissionConstants;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    /** Hard cap on how many rows can be requested for the tenant-linking search. */
    private static final int MAX_TENANT_SEARCH_SIZE = 50;

    /** Hard cap on how many rows can be requested for the user list. */
    private static final int MAX_LIST_SIZE = 200;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!id.equals(currentUserId)) {
            securityUtils.requirePermission(PermissionConstants.USER_VIEW);
            requireOwner();
        }
        User user = findEntityById(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        User user = userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(String search, UserStatus status, Pageable pageable) {
        securityUtils.requirePermission(PermissionConstants.USER_VIEW);
        requireOwner();
        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE),
                pageable.getSort());
        Page<User> page = userRepository.findAllWithFilters(normalize(search), status, capped);
        List<UserResponse> content = page.getContent().stream()
                .map(userMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getUsersSummary() {
        securityUtils.requirePermission(PermissionConstants.USER_VIEW);
        requireOwner();
        List<Object[]> rows = userRepository.countGroupedByStatus();
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0L;
        for (Object[] row : rows) {
            String status = row[0].toString();
            long cnt = ((Number) row[1]).longValue();
            counts.put(status, cnt);
            total += cnt;
        }
        return new UserSummaryResponse(total, counts);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!id.equals(currentUserId)) {
            securityUtils.requirePermission(PermissionConstants.USER_MANAGE);
            requireOwner();
        }
        User user = findEntityById(id);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        securityUtils.requirePermission(PermissionConstants.USER_DELETE);
        requireOwner();
        User user = findEntityById(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserTenantCandidateResponse> searchAvailableForTenant(String q, Pageable pageable) {
        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_TENANT_SEARCH_SIZE),
                pageable.getSort());

        Page<User> page = userRepository.findAvailableForTenant(normalize(q), capped);
        List<UserTenantCandidateResponse> content = page.getContent().stream()
                .map(UserServiceImpl::toCandidate)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private static String normalize(String q) {
        return q == null ? null : q.trim();
    }

    private static UserTenantCandidateResponse toCandidate(User user) {
        return new UserTenantCandidateResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + usernameOrEmail));
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(ROLE_USER)
                .accountLocked(user.getStatus() != com.example.spring_boot_project_api.model.UserStatus.ACTIVE)
                .build();
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.forId(id));
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
    }
}
