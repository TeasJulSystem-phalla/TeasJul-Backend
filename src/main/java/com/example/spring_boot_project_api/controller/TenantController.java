package com.example.spring_boot_project_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.request.UpdateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantMeResponse;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.dto.response.TenantUtilityResponse;
import com.example.spring_boot_project_api.service.TenantPortalService;
import com.example.spring_boot_project_api.service.TenantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant management endpoints (owner is resolved from the authenticated user)")
public class TenantController {

    private final TenantService tenantService;
    private final TenantPortalService tenantPortalService;

    @Operation(summary = "Get my tenant portal profile",
            description = "Returns the tenant profile linked to the currently authenticated user "
                    + "together with the active rental contract, room and house, if any.")
    @GetMapping("/me")
    public ResponseEntity<TenantMeResponse> getMyPortalProfile() {
        return ResponseEntity.ok(tenantPortalService.getMyTenantProfile());
    }

    @Operation(summary = "Update my tenant portal profile",
            description = "Updates the tenant profile linked to the currently authenticated user. "
                    + "Only the fields that are provided will be updated.")
    @PutMapping("/me")
    public ResponseEntity<TenantMeResponse> updateMyPortalProfile(
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantPortalService.updateMyTenantProfile(request));
    }

    @Operation(summary = "Get my utility usage",
            description = "Returns the active utility meters of the room tied to the current tenant's "
                    + "active rental contract, together with their latest reading.")
    @GetMapping("/me/utilities")
    public ResponseEntity<List<TenantUtilityResponse>> getMyUtilities() {
        return ResponseEntity.ok(tenantPortalService.getMyUtilities());
    }

    @Operation(summary = "Create a tenant",
            description = "Creates a tenant owned by the currently authenticated user. "
                    + "The user id must reference an existing user account and can only have one tenant profile. "
                    + "Tenant code and phone must be unique per owner.")
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.createTenant(request));
    }

    @Operation(summary = "List my tenants",
            description = "Returns all tenants belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<TenantResponse>> getMyTenants() {
        return ResponseEntity.ok(tenantService.getMyTenants());
    }

    @Operation(summary = "Get a tenant by id",
            description = "Returns the tenant with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @Operation(summary = "Update a tenant",
            description = "Updates the tenant with the given id if it belongs to the currently authenticated user. "
                    + "Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> updateTenant(@PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @Operation(summary = "Deactivate a tenant",
            description = "Marks the tenant with the given id as INACTIVE if it belongs to the currently authenticated user.")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateTenant(@PathVariable Long id) {
        tenantService.deactivateTenant(id);
        return ResponseEntity.noContent().build();
    }
}
