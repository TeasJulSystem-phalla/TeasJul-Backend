package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateDepositRequest;
import com.example.spring_boot_project_api.dto.request.UpdateDepositPaymentRequest;
import com.example.spring_boot_project_api.dto.response.ContractDepositResponse;
import com.example.spring_boot_project_api.enums.ContractDepositStatus;
import com.example.spring_boot_project_api.model.ContractDeposit;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.repository.ContractDepositRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.ContractDepositService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractDepositServiceImpl implements ContractDepositService {
    private final ContractDepositRepository depositRepository;
    private final RentalContractRepository contractRepository;

    @Override
    @Transactional
    public ContractDepositResponse createDeposit(CreateDepositRequest request) {
        RentalContract contract = contractRepository.findById(request.contractId())
                .orElseThrow(() -> new EntityNotFoundException("Contract not found with id: " + request.contractId()));

        ContractDeposit deposit = ContractDeposit.builder()
                .rentalContract(contract)
                .amount(request.amount())
                .notes(request.notes())
                .status(ContractDepositStatus.PENDING)
                .build();

        ContractDeposit saved = depositRepository.save(deposit);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ContractDepositResponse markAsPaid(Long depositId, UpdateDepositPaymentRequest request) {
        ContractDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new EntityNotFoundException("Deposit not found with id: " + depositId));

        deposit.setPaidAmount(request.paidAmount());
        deposit.setPaidAt(LocalDateTime.now());
        deposit.setStatus(ContractDepositStatus.PAID);

        ContractDeposit updated = depositRepository.save(deposit);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractDepositResponse> getDepositsByContract(Long contractId) {
        return depositRepository.findByRentalContractId(contractId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractDepositResponse getDepositById(Long depositId) {
        ContractDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new EntityNotFoundException("Deposit not found with id: " + depositId));

        return mapToResponse(deposit);
    }

    private ContractDepositResponse mapToResponse(ContractDeposit deposit) {
        return new ContractDepositResponse(
                deposit.getId(),
                deposit.getRentalContract().getId(),
                deposit.getAmount(),
                deposit.getPaidAmount(),
                deposit.getStatus(),
                deposit.getPaidAt(),
                deposit.getRefundedAt(),
                deposit.getNotes(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt()
        );
    }
}
