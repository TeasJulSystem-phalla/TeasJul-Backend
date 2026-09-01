package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RentalContractNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.RentalContractService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalContractServiceImpl implements RentalContractService {
    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RentalContractMapper contractMapper;
    private final SecurityUtils securityUtils;

    @Override
    public RentalContractResponse createContract(RentalContractRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Room targetRoom = resolveAccessibleRoom(request.getRoomId(), currentUserId);

        if (targetRoom.getStatus() == RoomStatus.OCCUPIED || contractRepository.existsByRoomIdAndStatus(request.getRoomId(), RentalContractStatus.ACTIVE)) {
            throw new DuplicateResourceException("Room " + targetRoom.getRoomNumber() + " is already occupied and cannot be assigned to a new contract.");
        }

        Long ownerId = targetRoom.getOwnerId();
        contractRepository.findByOwnerIdAndContractNo(ownerId, request.getContractNo())
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "Contract number already exists for this owner: " + request.getContractNo());
                });

        RentalContract contract = contractMapper.toEntity(request);
        contract.setOwnerId(ownerId);
        RentalContract savedContract = contractRepository.save(contract);
        syncRoomStatus(savedContract.getRoomId());
        return contractMapper.toResponse(savedContract);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalContractResponse getContractById(Long id) {
        return contractMapper.toResponse(getAccessibleContract(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByOwner(Long ownerId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<RentalContract> contracts = getScopedContracts(currentUserId, null);
        return contracts.stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByRoom(Long roomId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Room room = resolveAccessibleRoom(roomId, currentUserId);
        return contractRepository.findByOwnerIdAndRoomId(room.getOwnerId(), roomId)
                .stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalContractResponse> getContractsByStatus(RentalContractStatus status) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<RentalContract> contracts = getScopedContracts(currentUserId, status);
        return contracts.stream()
                .map(contractMapper::toResponse)
                .toList();
    }

    @Override
    public RentalContractResponse updateContract(Long id, RentalContractRequest request) {
        RentalContract existingContract = getAccessibleContract(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        Room targetRoom = resolveAccessibleRoom(request.getRoomId(), currentUserId);

        if (!existingContract.getRoomId().equals(request.getRoomId())) {
            Room newRoom = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> RoomNotFoundException.forId(request.getRoomId()));
            if (newRoom.getStatus() == RoomStatus.OCCUPIED || contractRepository.existsByRoomIdAndStatus(request.getRoomId(), RentalContractStatus.ACTIVE)) {
                throw new DuplicateResourceException("Room " + newRoom.getRoomNumber() + " is already occupied and cannot be assigned to this contract.");
            }
        }

        Long ownerId = targetRoom.getOwnerId();
        contractRepository.findByOwnerIdAndContractNo(ownerId, request.getContractNo())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new DuplicateResourceException(
                            "Contract number already exists for this owner: " + request.getContractNo());
                });

        Long prevRoomId = existingContract.getRoomId();
        contractMapper.updateEntityFromDto(request, existingContract);
        existingContract.setOwnerId(ownerId);
        RentalContract updatedContract = contractRepository.save(existingContract);
        syncRoomStatus(prevRoomId);
        if (!prevRoomId.equals(updatedContract.getRoomId())) {
            syncRoomStatus(updatedContract.getRoomId());
        }
        return contractMapper.toResponse(updatedContract);
    }

    @Override
    public RentalContractResponse updateStatus(Long id, RentalContractStatus status) {
        RentalContract contract = getAccessibleContract(id);
        contract.setStatus(status);
        RentalContract saved = contractRepository.save(contract);
        syncRoomStatus(contract.getRoomId());
        return contractMapper.toResponse(saved);
    }

    @Override
    public void deleteContract(Long id) {
        RentalContract contract = getAccessibleContract(id);
        Long roomId = contract.getRoomId();
        contractRepository.delete(contract);
        syncRoomStatus(roomId);
    }

    private void syncRoomStatus(Long roomId) {
        if (roomId == null) return;
        roomRepository.findById(roomId).ifPresent(room -> {
            boolean hasActiveContract = contractRepository.existsByRoomIdAndStatus(roomId, RentalContractStatus.ACTIVE);
            if (hasActiveContract && room.getStatus() == RoomStatus.AVAILABLE) {
                room.setStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
            } else if (!hasActiveContract && room.getStatus() == RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        });
    }

    private RentalContract getAccessibleContract(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return contractRepository.findById(id)
                .map(contract -> {
                    if (canAccess(contract.getOwnerId(), contract.getRoomId(), currentUserId)) {
                        return contract;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RentalContractNotFoundException.forId(id));
    }

    private Room resolveAccessibleRoom(Long roomId, Long currentUserId) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    if (canAccess(room.getOwnerId(), room.getHouseId(), currentUserId)) {
                        return room;
                    }
                    throw new AccessDeniedException(
                            "You do not have permission to access this resource");
                })
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));
    }

    private boolean canAccess(Long ownerId, Long houseId, Long currentUserId) {
        return ownerId.equals(currentUserId)
                || (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                        && userHouseAccessRepository.existsByUserIdAndHouseId(currentUserId, houseId));
    }

    private List<RentalContract> getScopedContracts(Long currentUserId, RentalContractStatus status) {
        if (securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            List<Long> assignedHouseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (assignedHouseIds.isEmpty()) {
                return Collections.emptyList();
            }
            if (status != null) {
                return contractRepository.findByRoomHouseIdInAndStatus(assignedHouseIds, status);
            }
            return contractRepository.findByRoomHouseIdIn(assignedHouseIds);
        }
        if (status != null) {
            return contractRepository.findByOwnerIdAndStatus(currentUserId, status);
        }
        return contractRepository.findByOwnerId(currentUserId);
    }
}
