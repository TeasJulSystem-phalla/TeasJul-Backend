package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.LoginRequest;
import com.example.spring_boot_project_api.dto.request.RegisterRequest;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
