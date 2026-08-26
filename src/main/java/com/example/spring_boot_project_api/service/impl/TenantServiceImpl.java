package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.TenantService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMapper tenantMapper;

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        User owner = getCurrentOwner();
        User tenantUser = userRepository.findById(request.userId())
                .orElseThrow(() -> UserNotFoundException.forId(request.userId()));

        if (tenantRepository.findByUserId(tenantUser.getId()).isPresent()) {
            throw new DuplicateResourceException(
                    "User already has a tenant profile: " + tenantUser.getUsername());
        }

        validateUniqueFields(owner.getId(), request.tenantCode(), request.phone(), null);

        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setUser(tenantUser);
        tenant.setOwner(owner);

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getMyTenants() {
        User owner = getCurrentOwner();
        return tenantRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(tenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long id) {
        return tenantMapper.toResponse(getOwnedTenant(id));
    }

    @Override
    @Transactional
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = getOwnedTenant(id);

        if (request.phone() != null) {
            if (tenantRepository.existsByOwnerIdAndPhoneAndIdNot(tenant.getOwner().getId(),
                    request.phone(), tenant.getId())) {
                throw new DuplicateResourceException(
                        "Tenant phone already exists for this owner: " + request.phone());
            }
            tenant.setPhone(request.phone());
        }
        if (request.firstName() != null) {
            tenant.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            tenant.setLastName(request.lastName());
        }
        if (request.gender() != null) {
            tenant.setGender(request.gender());
        }
        if (request.dateOfBirth() != null) {
            tenant.setDateOfBirth(request.dateOfBirth());
        }
        if (request.email() != null) {
            tenant.setEmail(request.email());
        }
        if (request.occupationType() != null) {
            tenant.setOccupationType(request.occupationType());
        }
        if (request.occupation() != null) {
            tenant.setOccupation(request.occupation());
        }
        if (request.schoolOrCompany() != null) {
            tenant.setSchoolOrCompany(request.schoolOrCompany());
        }
        if (request.idType() != null) {
            tenant.setIdType(request.idType());
        }
        if (request.idNumber() != null) {
            tenant.setIdNumber(request.idNumber());
        }
        if (request.address() != null) {
            tenant.setAddress(request.address());
        }

        return tenantMapper.toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public void deactivateTenant(Long id) {
        Tenant tenant = getOwnedTenant(id);
        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }

    private Tenant getOwnedTenant(Long id) {
        Long currentUserId = getCurrentOwner().getId();

        return tenantRepository.findByIdAndOwnerId(id, currentUserId)
                .orElseThrow(() -> TenantNotFoundException.forId(id));
    }

    private void validateUniqueFields(Long ownerId, String tenantCode, String phone, Long tenantId) {
        boolean isCreate = tenantId == null;

        if (tenantCode != null && (isCreate
                ? tenantRepository.existsByOwnerIdAndTenantCode(ownerId, tenantCode)
                : tenantRepository.existsByOwnerIdAndTenantCodeAndIdNot(ownerId, tenantCode, tenantId))) {
            throw new DuplicateResourceException(
                    "Tenant code already exists for this owner: " + tenantCode);
        }
        if (phone != null && (isCreate
                ? tenantRepository.existsByOwnerIdAndPhone(ownerId, phone)
                : tenantRepository.existsByOwnerIdAndPhoneAndIdNot(ownerId, phone, tenantId))) {
            throw new DuplicateResourceException(
                    "Tenant phone already exists for this owner: " + phone);
        }
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
