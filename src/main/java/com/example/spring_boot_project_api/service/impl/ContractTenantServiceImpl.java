package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ContractTenantRequest;
import com.example.spring_boot_project_api.dto.response.ContractTenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.mapper.ContractTenantMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ContractTenantService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractTenantServiceImpl implements ContractTenantService {
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository contractRepository;
    private final UserRepository userRepository;
    private final ContractTenantMapper contractTenantMapper;

    @Override
    @Transactional
    public ContractTenantResponse addTenantToContract(ContractTenantRequest request) {
        RentalContract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + request.contractId()));
        User tenant = userRepository.findById(request.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with id: " + request.tenantId()));

        if (contractTenantRepository.existsByRentalContractIdAndTenantId(contract.getId(), tenant.getId())) {
            throw new DuplicateResourceException("Tenant " + tenant.getId() + " is already added to contract "
                    + contract.getId());
        }

        ContractTenant saved = contractTenantRepository.save(contractTenantMapper.toEntity(
                contract, tenant, request.isPrimary(), LocalDate.now()));

        return contractTenantMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractTenantResponse getContractTenantById(Long id) {
        return contractTenantRepository.findById(id)
                .map(contractTenantMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Contract tenant not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getTenantsByContract(Long contractId) {
        return contractTenantRepository.findByRentalContractId(contractId).stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractTenantResponse> getContractsByTenant(Long tenantId) {
        return contractTenantRepository.findByTenantId(tenantId).stream()
                .map(contractTenantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ContractTenantResponse updateContractTenant(Long id, ContractTenantRequest request) {
        ContractTenant contractTenant = contractTenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contract tenant not found with id: " + id));

        RentalContract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + request.contractId()));
        User tenant = userRepository.findById(request.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with id: " + request.tenantId()));

        boolean assignmentChanged = !contractTenant.getRentalContract().getId().equals(contract.getId())
                || !contractTenant.getTenant().getId().equals(tenant.getId());
        if (assignmentChanged
                && contractTenantRepository.existsByRentalContractIdAndTenantId(contract.getId(), tenant.getId())) {
            throw new DuplicateResourceException("Tenant " + tenant.getId() + " is already added to contract "
                    + contract.getId());
        }

        contractTenant.setRentalContract(contract);
        contractTenant.setTenant(tenant);
        if (request.isPrimary() != null) {
            contractTenant.setIsPrimary(request.isPrimary());
        }

        ContractTenant updated = contractTenantRepository.save(contractTenant);
        return contractTenantMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void removeTenantFromContract(Long id) {
        if (!contractTenantRepository.existsById(id)) {
            throw new EntityNotFoundException("Contract tenant not found with id: " + id);
        }
        contractTenantRepository.deleteById(id);
    }
}
