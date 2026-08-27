package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.ContractTenantNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.mapper.ContractTenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTenantServiceImpl implements ContractTenantService {
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository contractRepository;
    private final TenantRepository tenantRepository;
    private final ContractTenantMapper contractTenantMapper;
    private final SecurityUtils securityUtils;

    @Override
    public ContractTenantResponse addTenantToContract(ContractTenantRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        RentalContract contract = getOwnedContract(request.contractId());
        verifyTenantOwnership(request.tenantId(), currentUserId);

        if (contractTenantRepository.existsByContractIdAndTenantId(request.contractId(), request.tenantId())) {
            throw new DuplicateResourceException("Tenant is already assigned to this contract.");
        }

        ContractTenant entity = contractTenantMapper.toEntity(request);
        ContractTenant saved = contractTenantRepository.save(entity);

        // Once a tenant is assigned, a draft contract becomes active automatically.
        if (contract.getStatus() == RentalContractStatus.DRAFT) {
            contract.setStatus(RentalContractStatus.ACTIVE);
            contractRepository.save(contract);
        }

        return contractTenantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTenantResponse getContractTenantById(Long id) {
        return contractTenantMapper.toResponse(getOwnedContractTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getTenantsByContract(Long contractId) {
        getOwnedContract(contractId);
        return contractTenantRepository.findByContractId(contractId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getContractsByTenant(Long tenantId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyTenantOwnership(tenantId, currentUserId);
        return contractTenantRepository.findByTenantId(tenantId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    public ContractTenantResponse updateContractTenant(Long id, ContractTenantRequest request) {
        ContractTenant existing = getOwnedContractTenant(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        getOwnedContract(request.contractId());
        verifyTenantOwnership(request.tenantId(), currentUserId);

        contractTenantRepository.findByContractIdAndTenantId(request.contractId(), request.tenantId())
                .filter(ct -> !ct.getId().equals(id))
                .ifPresent(ct -> {
                    throw new DuplicateResourceException("Tenant is already assigned to this contract.");
                });

        contractTenantMapper.updateEntityFromDto(request, existing);
        return contractTenantMapper.toResponse(contractTenantRepository.save(existing));
    }

    @Override
    public void removeTenantFromContract(Long id) {
        ContractTenant existing = getOwnedContractTenant(id);
        contractTenantRepository.delete(existing);
    }

    private ContractTenant getOwnedContractTenant(Long id) {
        return contractTenantRepository.findById(id)
                .map(ct -> {
                    getOwnedContract(ct.getContractId());
                    return ct;
                })
                .orElseThrow(() -> ContractTenantNotFoundException.forId(id));
    }

    private RentalContract getOwnedContract(Long contractId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return contractRepository.findById(contractId)
                .map(contract -> {
                    if (!contract.getOwnerId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return contract;
                })
                .orElseThrow(() -> RentalContractNotFoundException.forId(contractId));
    }

    private void verifyTenantOwnership(Long tenantId, Long currentUserId) {
        tenantRepository.findByIdAndOwnerId(tenantId, currentUserId)
                .orElseThrow(() -> TenantNotFoundException.forId(tenantId));
    }
}
