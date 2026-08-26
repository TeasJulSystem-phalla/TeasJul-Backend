package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.service.RentalContractService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalContractServiceImpl implements RentalContractService {
    private final RentalContractRepository contractRepository;
    private final RentalContractMapper contractMapper;

    @Override
    public RentalContractResponse createContract(RentalContractRequest request) {
        contractRepository.findByOwnerIdAndContractNo(request.getOwnerId(), request.getContractNo())
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Contract number already exists for this owner.");
                });

        RentalContract contract = contractMapper.toEntity(request);
        RentalContract savedContract = contractRepository.save(contract);
        return contractMapper.toResponse(savedContract);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalContractResponse getContractById(Long id) {
        RentalContract contract = findEntityById(id);
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByOwner(Long ownerId) {
        return contractRepository.findByOwnerId(ownerId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByRoom(Long roomId) {
        return contractRepository.findByRoomId(roomId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByStatus(RentalContractStatus status) {
        return contractRepository.findByStatus(status)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    public RentalContractResponse updateContract(Long id, RentalContractRequest request) {
        RentalContract existingContract = findEntityById(id);

        contractRepository.findByOwnerIdAndContractNo(request.getOwnerId(), request.getContractNo())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("Contract number already exists for this owner.");
                });

        contractMapper.updateEntityFromDto(request, existingContract);
        RentalContract updatedContract = contractRepository.save(existingContract);
        return contractMapper.toResponse(updatedContract);
    }

    @Override
    public RentalContractResponse updateStatus(Long id, RentalContractStatus status) {
        RentalContract contract = findEntityById(id);
        contract.setStatus(status);
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Override
    public void deleteContract(Long id) {
        RentalContract contract = findEntityById(id);
        contractRepository.delete(contract);
    }

    private RentalContract findEntityById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rental contract not found with ID: " + id));
    }
}
