package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.mapper.UserMapper;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.UserService;
import com.example.spring_boot_project_api.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userService.createUser(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request.usernameOrEmail(), request.password());

        user.setLastLoginAt(LocalDateTime.now());
        user = userService.createUser(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationMs(),
                userMapper.toResponse(user));
    }

    private User authenticate(String usernameOrEmail, String rawPassword) {
        User user = findUserByUsernameOrEmail(usernameOrEmail);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new LockedException("Account is locked or inactive");
        }
        return user;
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }
}
