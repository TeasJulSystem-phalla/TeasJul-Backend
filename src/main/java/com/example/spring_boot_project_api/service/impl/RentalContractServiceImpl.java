package com.example.spring_boot_project_api.service.impl;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.ContractSummaryResponse;
import com.example.spring_boot_project_api.dto.response.ExpiringContractSummary;
import com.example.spring_boot_project_api.dto.response.PageResponse;
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
import com.example.spring_boot_project_api.service.NotificationService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalContractServiceImpl implements RentalContractService {
    private static final Logger log = LoggerFactory.getLogger(RentalContractServiceImpl.class);

    private static final int MAX_LIST_SIZE = 200;
    private static final int MAX_EXPIRING_SOON = 20;

    private final RentalContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final UserHouseAccessRepository userHouseAccessRepository;
    private final RentalContractMapper contractMapper;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

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
        try {
            notificationService.notifyContractExpiring(savedContract);
        } catch (RuntimeException e) {
            log.warn("Failed to create contract-expiry notification", e);
        }
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
    public PageResponse<RentalContractResponse> getContractsByOwner(Long ownerId, String search,
            RentalContractStatus status, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean manager = securityUtils.hasRole(SecurityUtils.ROLE_MANAGER);

        Pageable capped = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_LIST_SIZE),
                pageable.getSort());
        String q = normalize(search);

        Page<RentalContract> page;
        if (manager) {
            List<Long> houseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (houseIds.isEmpty()) {
                return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0L, 0);
            }
            page = contractRepository.findScopedByHouseIds(houseIds, q, status, capped);
        } else {
            page = contractRepository.findScopedByOwnerId(currentUserId, q, status, capped);
        }

        List<RentalContractResponse> content = page.getContent().stream()
                .map(contractMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
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
    public PageResponse<RentalContractResponse> getContractsByStatus(RentalContractStatus status, Pageable pageable) {
        return getContractsByOwner(null, null, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSummaryResponse getContractsSummary() {
        Long currentUserId = securityUtils.getCurrentUserId();
        boolean manager = securityUtils.hasRole(SecurityUtils.ROLE_MANAGER);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        long total = 0L;
        BigDecimal activeMonthlyRent = BigDecimal.ZERO;
        List<ExpiringContractSummary> expiring;
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(60);
        PageRequest expiringPage = PageRequest.of(0, MAX_EXPIRING_SOON);

        if (manager) {
            List<Long> houseIds = userHouseAccessRepository.findHouseIdsByUserId(currentUserId);
            if (houseIds.isEmpty()) {
                return new ContractSummaryResponse(0L, Map.of(), BigDecimal.ZERO, List.of());
            }
            for (Object[] row : contractRepository.countGroupByHouseIds(houseIds)) {
                statusCounts.put(row[0].toString(), ((Number) row[1]).longValue());
            }
            long sCount = 0L;
            for (long v : statusCounts.values()) sCount += v;
            total = sCount;
            activeMonthlyRent = contractRepository.sumActiveMonthlyRentByHouseIds(houseIds);
            expiring = contractRepository.findExpiringSoonByHouseIds(houseIds, today, windowEnd, expiringPage);
        } else {
            for (Object[] row : contractRepository.countGroupByOwnerId(currentUserId)) {
                statusCounts.put(row[0].toString(), ((Number) row[1]).longValue());
            }
            for (long v : statusCounts.values()) total += v;
            activeMonthlyRent = contractRepository.sumActiveMonthlyRentByOwnerId(currentUserId);
            expiring = contractRepository.findExpiringSoonByOwnerId(currentUserId, today, windowEnd, expiringPage);
        }

        return new ContractSummaryResponse(total, statusCounts, activeMonthlyRent, expiring);
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
        try {
            notificationService.notifyContractExpiring(updatedContract);
        } catch (RuntimeException e) {
            log.warn("Failed to create contract-expiry notification", e);
        }
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

    private static String normalize(String q) {
        return q == null ? null : q.trim();
    }
}
