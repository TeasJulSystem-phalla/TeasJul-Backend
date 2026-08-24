package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.spring_boot_project_api.dto.request.CreatePermissionRequest;
import com.example.spring_boot_project_api.dto.request.UpdatePermissionRequest;
import com.example.spring_boot_project_api.dto.response.PermissionResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.PermissionNotFoundException;
import com.example.spring_boot_project_api.mapper.PermissionMapper;
import com.example.spring_boot_project_api.model.Permission;
import com.example.spring_boot_project_api.repository.PermissionRepository;
import com.example.spring_boot_project_api.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Permission code already exists: " + request.code());
        }

        return permissionMapper.toResponse(permissionRepository.save(permissionMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        return permissionMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions(String module) {
        List<Permission> permissions = StringUtils.hasText(module)
                ? permissionRepository.findByModuleIgnoreCase(module)
                : permissionRepository.findAll();

        return permissions.stream()
                .map(permissionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        Permission permission = getEntityById(id);

        if (request.code() != null && permissionRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Permission code already exists: " + request.code());
        }

        if (request.name() != null) {
            permission.setName(request.name());
        }
        if (request.code() != null) {
            permission.setCode(request.code());
        }
        if (request.module() != null) {
            permission.setModule(request.module());
        }
        if (request.description() != null) {
            permission.setDescription(request.description());
        }

        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        permissionRepository.delete(getEntityById(id));
    }

    private Permission getEntityById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> PermissionNotFoundException.forId(id));
    }
}
