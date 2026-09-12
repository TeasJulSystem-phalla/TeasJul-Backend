package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;
import com.example.spring_boot_project_api.model.OwnerSetting;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.OwnerSettingRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.OwnerSettingServiceImpl;
import com.example.spring_boot_project_api.service.impl.SecurityAuditLogger;
import com.example.spring_boot_project_api.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OwnerSettingServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock private OwnerSettingRepository ownerSettingRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private SecurityAuditLogger securityAuditLogger;

    @InjectMocks private OwnerSettingServiceImpl ownerSettingService;

    private User owner;
    private OwnerSetting setting;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(5L).username("owner1").email("owner@test.com").build();
        setting = OwnerSetting.builder()
                .id(1L)
                .owner(owner)
                .settings(themeJson("LIGHT", "#111111", "#222222"))
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(owner);
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(true);
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.of(setting));
        when(ownerSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(userHouseAccessRepository.findOwnerIdsByUserId(any())).thenReturn(List.of());
    }

    private String themeJson(String mode, String primary, String secondary) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();
            root.put("exchangeRate", 4100.0);
            ObjectNode theme = root.putObject("theme");
            theme.put("themeMode", mode);
            theme.put("primaryColor", primary);
            theme.put("secondaryColor", secondary);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getTheme_returnsDefaults_whenNoSettingsRowForOwner() {
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.empty());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
        assertThat(response.secondaryColor()).isEqualTo("#0F172A");
    }

    @Test
    void getTheme_returnsSavedThemeFromThemeBlock() {
        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#111111");
        assertThat(response.secondaryColor()).isEqualTo("#222222");
    }

    @Test
    void getTheme_forManager_resolvesOrganizationOwnerTheme() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(false);
        when(userHouseAccessRepository.findOwnerIdsByUserId(9L)).thenReturn(List.of(5L));
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("manager1").build());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void getTheme_forTenant_resolvesOrganizationOwnerTheme() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(true);
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("tenant1").build());
        when(tenantRepository.findByUserId(9L)).thenReturn(
                Optional.of(Tenant.builder().id(10L).user(
                        User.builder().id(9L).build()).owner(owner).build()));

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository).findByOwnerId(5L);
        assertThat(response.ownerId()).isEqualTo(5L);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void getTheme_forUserWithoutOrganization_returnsDefaults() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);
        when(securityUtils.hasRole(SecurityUtils.ROLE_TENANT)).thenReturn(false);
        when(userHouseAccessRepository.findOwnerIdsByUserId(9L)).thenReturn(List.of());
        when(securityUtils.getCurrentUser()).thenReturn(
                User.builder().id(9L).username("staff1").build());

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        verify(ownerSettingRepository, never()).findByOwnerId(any());
        assertThat(response.ownerId()).isNull();
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
    }

    @Test
    void getTheme_fallsBackToBrandingColor_whenThemeBlockMissing() {
        OwnerSetting brandingOnly = OwnerSetting.builder()
                .id(2L)
                .owner(owner)
                .settings("{\"primaryColor\":\"#3b82f6\",\"secondaryColor\":\"#10b981\"}")
                .build();
        when(ownerSettingRepository.findByOwnerId(5L)).thenReturn(Optional.of(brandingOnly));

        ThemeSettingsResponse response = ownerSettingService.getTheme();

        assertThat(response.primaryColor()).isEqualTo("#3B82F6");
        assertThat(response.secondaryColor()).isEqualTo("#10B981");
        assertThat(response.themeMode()).isEqualTo("LIGHT");
    }

    @Test
    void updateTheme_persistsThemeAndAudits() {
        ThemeSettingsResponse response = ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00"));

        verify(ownerSettingRepository).save(setting);
        assertThat(setting.getSettings())
                .contains("\"themeMode\":\"DARK\"")
                .contains("\"primaryColor\":\"#FF0000\"")
                .contains("\"secondaryColor\":\"#00FF00\"");
        verify(securityAuditLogger).log(eq(5L), eq("THEME_UPDATED"), any());
        assertThat(response.themeMode()).isEqualTo("DARK");
        assertThat(response.primaryColor()).isEqualTo("#FF0000");
    }

    @Test
    void updateTheme_byNonOwner_isForbidden() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.updateTheme(
                new UpdateThemeRequest("DARK", "#ff0000", "#00ff00")))
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
        verify(securityAuditLogger, never()).log(any(), any(), any());
    }

    @Test
    void resetTheme_resetsToDefaultsAndAudits() {
        ThemeSettingsResponse response = ownerSettingService.resetTheme();

        verify(ownerSettingRepository).save(setting);
        assertThat(response.themeMode()).isEqualTo("LIGHT");
        assertThat(response.primaryColor()).isEqualTo("#2563EB");
        assertThat(response.secondaryColor()).isEqualTo("#0F172A");
        verify(securityAuditLogger).log(eq(5L), eq("THEME_RESET"), any());
    }

    @Test
    void resetTheme_byNonOwner_isForbidden() {
        when(securityUtils.hasRole(SecurityUtils.ROLE_OWNER)).thenReturn(false);

        assertThatThrownBy(() -> ownerSettingService.resetTheme())
                .isInstanceOf(AccessDeniedException.class);

        verify(ownerSettingRepository, never()).save(any());
    }

    @Test
    void updateSettings_preservesExistingThemeBlock() {
        updateThemeSettings();

        assertThat(setting.getSettings())
                .contains("\"themeMode\":\"DARK\"")
                .contains("\"primaryColor\":\"#FF0000\"")
                .contains("exchangeRate\":4100");
    }

    private void updateThemeSettings() {
        UpdateThemeRequest themeUpdate = new UpdateThemeRequest("DARK", "#ff0000", "#00ff00");
        ownerSettingService.updateTheme(themeUpdate);
        ownerSettingService.updateSettings(fullSettingsRequest());
    }

    private UpdateOwnerSettingsRequest fullSettingsRequest() {
        return new UpdateOwnerSettingsRequest(
                "USD", "USD", new BigDecimal("4100"), "DD/MM/YYYY", "Asia/Phnom_Penh",
                "Business", "", "", "", "", "#3b82f6", "#10b981",
                "KM",
                "INV", 10, "", true, true, true, true,
                "កិច្ចសន្យាជួលផ្ទះ", "", "", "", "", "KM",
                "RCP", "KM", "", true, true, true, true,
                "", "");
    }
}