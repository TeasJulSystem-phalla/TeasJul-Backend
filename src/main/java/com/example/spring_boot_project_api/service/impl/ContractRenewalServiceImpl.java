package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.exception.ContractRenewalNotFoundException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.mapper.ContractRenewalMapper;
import com.example.spring_boot_project_api.model.ContractRenewal;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.ContractRenewalService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractRenewalServiceImpl implements ContractRenewalService {

    private final ContractRenewalRepository renewalRepository;
    private final RentalContractRepository contractRepository;
    private final ContractRenewalMapper renewalMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public ContractRenewalResponse createRenewal(CreateRenewalRequest request) {
        RentalContract contract = getOwnedContract(request.contractId());
        User createdBy = securityUtils.getCurrentUser();

        ContractRenewal saved = renewalRepository.save(renewalMapper.toEntity(contract, createdBy, request));
        return renewalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractRenewalResponse getRenewalById(Long id) {
        return renewalMapper.toResponse(getOwnedRenewal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRenewalResponse> getRenewalsByContract(Long contractId) {
        getOwnedContract(contractId);
        return renewalRepository.findByRentalContractIdOrderByRenewedAtDesc(contractId)
                .stream()
                .map(renewalMapper::toResponse)
                .toList();
    }

    private ContractRenewal assertOwnedAndGet(ContractRenewal renewal) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!renewal.getRentalContract().getOwnerId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this resource");
        }
        return renewal;
    }

    private ContractRenewal getOwnedRenewal(Long id) {
        ContractRenewal renewal = renewalRepository.findById(id)
                .orElseThrow(() -> ContractRenewalNotFoundException.forId(id));
        return assertOwnedAndGet(renewal);
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
}
