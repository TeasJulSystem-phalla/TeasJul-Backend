package com.example.spring_boot_project_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ContractTenantResponse(
    Long id,
    Long contractId,
    String contractNo,
    Long tenantId,
    String tenantUsername,
    String tenantFullName,
    Boolean isPrimary,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate joinedAt,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate leftAt,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt
) {}
