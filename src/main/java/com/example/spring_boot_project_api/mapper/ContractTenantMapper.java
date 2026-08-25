package com.example.spring_boot_project_api.mapper;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;

@Component
public class ContractTenantMapper {

    public ContractTenant toEntity(RentalContract rentalContract, User tenant, Boolean isPrimary, LocalDate joinedAt) {
        return ContractTenant.builder()
                .rentalContract(rentalContract)
                .tenant(tenant)
                .isPrimary(isPrimary != null && isPrimary)
                .joinedAt(joinedAt)
                .build();
    }

    public ContractTenantResponse toResponse(ContractTenant contractTenant) {
        RentalContract rentalContract = contractTenant.getRentalContract();
        User tenant = contractTenant.getTenant();

        return new ContractTenantResponse(
                contractTenant.getId(),
                rentalContract.getId(),
                rentalContract.getContractNo(),
                tenant.getId(),
                tenant.getUsername(),
                tenant.getFirstName() + " " + tenant.getLastName(),
                contractTenant.getIsPrimary(),
                contractTenant.getJoinedAt(),
                contractTenant.getLeftAt(),
                contractTenant.getCreatedAt(),
                contractTenant.getUpdatedAt());
    }
}
