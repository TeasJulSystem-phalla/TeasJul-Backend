package com.example.spring_boot_project_api.service;

import java.util.List;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantResponse;

public interface TenantService {

    TenantResponse createTenant(CreateTenantRequest request);

    List<TenantResponse> getMyTenants();

    TenantResponse getTenantById(Long id);

    TenantResponse updateTenant(Long id, UpdateTenantRequest request);

    void deactivateTenant(Long id);
}
