package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
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
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
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
        User user = findEntityById(id);
        userRepository.delete(user);
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
}
