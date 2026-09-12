package com.example.spring_boot_project_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import com.example.spring_boot_project_api.service.OwnerSettingService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/settings/theme")
@RequiredArgsConstructor
@Tag(name = "Theme Settings", description = "Organization system theme (appearance). Modifications are OWNER-only.")
public class ThemeController {

    private final OwnerSettingService ownerSettingService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "Get the organization theme (available to any authenticated user)")
    @GetMapping
    public ResponseEntity<ThemeSettingsResponse> getTheme() {
        return ResponseEntity.ok(ownerSettingService.getTheme());
    }

    @Operation(summary = "Update the organization theme (OWNER only)")
    @PutMapping
    public ResponseEntity<ThemeSettingsResponse> updateTheme(
            @Valid @RequestBody UpdateThemeRequest request) {
        requireOwner();
        return ResponseEntity.ok(ownerSettingService.updateTheme(request));
    }

    @Operation(summary = "Reset the organization theme to defaults (OWNER only)")
    @PostMapping("/reset")
    public ResponseEntity<ThemeSettingsResponse> resetTheme() {
        requireOwner();
        return ResponseEntity.ok(ownerSettingService.resetTheme());
    }

    private void requireOwner() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)) {
            throw new AccessDeniedException("Only owners can modify the theme");
        }
    }
}