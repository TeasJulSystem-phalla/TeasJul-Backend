package com.example.spring_boot_project_api.service;

import java.math.BigDecimal;

import com.example.spring_boot_project_api.dto.request.UpdateOwnerSettingsRequest;
import com.example.spring_boot_project_api.dto.request.UpdateThemeRequest;
import com.example.spring_boot_project_api.dto.response.OwnerSettingsResponse;
import com.example.spring_boot_project_api.dto.response.ThemeSettingsResponse;

public interface OwnerSettingService {

    OwnerSettingsResponse getSettings();

    OwnerSettingsResponse updateSettings(UpdateOwnerSettingsRequest request);

    ThemeSettingsResponse getTheme();

    ThemeSettingsResponse updateTheme(UpdateThemeRequest request);

    ThemeSettingsResponse resetTheme();

    BigDecimal getExchangeRate(Long ownerId);

    String getInvoicePrefix(Long ownerId);

    int getInvoiceDefaultDueDays(Long ownerId);

    String getTelegramBotToken(Long ownerId);

    String getTelegramChatId(Long ownerId);
}
