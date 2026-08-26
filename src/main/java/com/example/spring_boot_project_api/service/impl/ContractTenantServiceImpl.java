package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.mapper.ContractTenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractTenantServiceImpl implements ContractTenantService {
    private final ContractTenantRepository contractTenantRepository;
    private final ContractTenantMapper contractTenantMapper;

    @Override
    public ContractTenantResponse addTenantToContract(ContractTenantRequest request) {
        if (contractTenantRepository.existsByContractIdAndTenantId(request.contractId(), request.tenantId())) {
            throw new IllegalArgumentException("Tenant is already assigned to this contract.");
        }

        ContractTenant entity = contractTenantMapper.toEntity(request);
        ContractTenant saved = contractTenantRepository.save(entity);
        return contractTenantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTenantResponse getContractTenantById(Long id) {
        return contractTenantMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getTenantsByContract(Long contractId) {
        return contractTenantRepository.findByContractId(contractId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getContractsByTenant(Long tenantId) {
        return contractTenantRepository.findByTenantId(tenantId)
                .stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    public ContractTenantResponse updateContractTenant(Long id, ContractTenantRequest request) {
        ContractTenant existing = findEntityById(id);

        contractTenantRepository.findByContractIdAndTenantId(request.contractId(), request.tenantId())
                .filter(ct -> !ct.getId().equals(id))
                .ifPresent(ct -> {
                    throw new IllegalArgumentException("Tenant is already assigned to this contract.");
                });

        contractTenantMapper.updateEntityFromDto(request, existing);
        return contractTenantMapper.toResponse(contractTenantRepository.save(existing));
    }

    @Override
    public void removeTenantFromContract(Long id) {
        ContractTenant existing = findEntityById(id);
        contractTenantRepository.delete(existing);
    }

    private ContractTenant findEntityById(Long id) {
        return contractTenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract tenant relationship not found with ID: " + id));
    }
}
