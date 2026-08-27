package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalContractServiceImpl implements RentalContractService {
    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final RentalContractMapper contractMapper;
    private final SecurityUtils securityUtils;

    @Override
    public RentalContractResponse createContract(RentalContractRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyRoomOwnership(request.getRoomId(), currentUserId);

        contractRepository.findByOwnerIdAndContractNo(currentUserId, request.getContractNo())
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "Contract number already exists for this owner: " + request.getContractNo());
                });

        RentalContract contract = contractMapper.toEntity(request);
        contract.setOwnerId(currentUserId);
        RentalContract savedContract = contractRepository.save(contract);
        return contractMapper.toResponse(savedContract);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalContractResponse getContractById(Long id) {
        return contractMapper.toResponse(getOwnedContract(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByOwner(Long ownerId) {
        return contractRepository.findByOwnerId(securityUtils.getCurrentUserId())
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByRoom(Long roomId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyRoomOwnership(roomId, currentUserId);
        return contractRepository.findByOwnerIdAndRoomId(currentUserId, roomId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByStatus(RentalContractStatus status) {
        return contractRepository.findByOwnerIdAndStatus(securityUtils.getCurrentUserId(), status)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    public RentalContractResponse updateContract(Long id, RentalContractRequest request) {
        RentalContract existingContract = getOwnedContract(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyRoomOwnership(request.getRoomId(), currentUserId);

        contractRepository.findByOwnerIdAndContractNo(currentUserId, request.getContractNo())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "Contract number already exists for this owner: " + request.getContractNo());
                });

        contractMapper.updateEntityFromDto(request, existingContract);
        RentalContract updatedContract = contractRepository.save(existingContract);
        return contractMapper.toResponse(updatedContract);
    }

    @Override
    public RentalContractResponse updateStatus(Long id, RentalContractStatus status) {
        RentalContract contract = getOwnedContract(id);
        contract.setStatus(status);
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Override
    public void deleteContract(Long id) {
        RentalContract contract = getOwnedContract(id);
        contractRepository.delete(contract);
    }

    private RentalContract getOwnedContract(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return contractRepository.findById(id)
                .map(contract -> {
                    if (!contract.getOwnerId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return contract;
                })
                .orElseThrow(() -> RentalContractNotFoundException.forId(id));
    }

    private void verifyRoomOwnership(Long roomId, Long currentUserId) {
        roomRepository.findById(roomId)
                .filter(room -> room.getOwnerId().equals(currentUserId))
                .map(Room::getId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }
}
