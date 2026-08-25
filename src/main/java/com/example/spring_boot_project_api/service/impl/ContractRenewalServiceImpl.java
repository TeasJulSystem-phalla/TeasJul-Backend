package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateRenewalRequest;
import com.example.spring_boot_project_api.dto.response.ContractRenewalResponse;
import com.example.spring_boot_project_api.mapper.ContractRenewalMapper;
import com.example.spring_boot_project_api.model.ContractRenewal;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractRenewalRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.ContractRenewalService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractRenewalServiceImpl implements ContractRenewalService {

    private final ContractRenewalRepository renewalRepository;
    private final RentalContractRepository contractRepository;
    private final UserRepository userRepository;
    private final ContractRenewalMapper renewalMapper;

    @Override
    @Transactional
    public ContractRenewalResponse createRenewal(CreateRenewalRequest request) {
        RentalContract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + request.contractId()));

        User createdBy = null;
        if (request.createdById() != null) {
            createdBy = userRepository.findById(request.createdById())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.createdById()));
        }

        ContractRenewal saved = renewalRepository.save(renewalMapper.toEntity(contract, createdBy, request));
        return renewalMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractRenewalResponse getRenewalById(Long id) {
        return renewalRepository.findById(id)
                .map(renewalMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Contract renewal not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractRenewalResponse> getRenewalsByContract(Long contractId) {
        return renewalRepository.findByRentalContractIdOrderByRenewedAtDesc(contractId)
                .stream()
                .map(renewalMapper::toResponse)
                .toList();
    }
}
