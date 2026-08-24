package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateRoleRequest;
import com.example.spring_boot_project_api.dto.request.UpdateRoleRequest;
import com.example.spring_boot_project_api.dto.response.RoleResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoleNotFoundException;
import com.example.spring_boot_project_api.mapper.RoleMapper;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.service.RoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Role code already exists: " + request.code());
        }
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        return roleMapper.toResponse(roleRepository.save(roleMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        return roleMapper.toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = getEntityById(id);

        if (request.code() != null && roleRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Role code already exists: " + request.code());
        }
        if (request.name() != null && roleRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException("Role name already exists: " + request.name());
        }

        if (request.name() != null) {
            role.setName(request.name());
        }
        if (request.code() != null) {
            role.setCode(request.code());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }

        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getEntityById(id);

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new AccessDeniedException("System roles cannot be deleted");
        }

        roleRepository.delete(role);
    }

    private Role getEntityById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> RoleNotFoundException.forId(id));
    }
}
