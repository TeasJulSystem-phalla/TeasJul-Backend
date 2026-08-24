package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.UpdateUserRequest;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.model.User;

public interface UserService {

    User createUser(User user);

    UserResponse getUserById(Long id);

    UserResponse getCurrentUser();

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);
}
