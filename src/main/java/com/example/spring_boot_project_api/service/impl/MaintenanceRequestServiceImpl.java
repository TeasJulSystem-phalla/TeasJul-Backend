package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.ChangeMaintenanceRequestStatusRequest;
import com.example.spring_boot_project_api.dto.request.CreateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.request.UpdateMaintenanceRequestRequest;
import com.example.spring_boot_project_api.dto.response.MaintenanceRequestResponse;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.InvalidMaintenanceOperationException;
import com.example.spring_boot_project_api.exception.MaintenanceCategoryNotFoundException;
import com.example.spring_boot_project_api.exception.MaintenanceRequestNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.exception.TenantNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.MaintenanceRequestMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.MaintenanceCategory;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.MaintenanceRequestPriority;
import com.example.spring_boot_project_api.model.MaintenanceRequestStatus;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.MaintenanceCategoryRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.MaintenanceRequestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaintenanceRequestServiceImpl implements MaintenanceRequestService {

    private static final DateTimeFormatter REQUEST_NO_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Map<MaintenanceRequestStatus, List<MaintenanceRequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            MaintenanceRequestStatus.PENDING,
            List.of(MaintenanceRequestStatus.IN_PROGRESS, MaintenanceRequestStatus.CANCELLED),
            MaintenanceRequestStatus.IN_PROGRESS,
            List.of(MaintenanceRequestStatus.RESOLVED),
            MaintenanceRequestStatus.RESOLVED,
            List.of(),
            MaintenanceRequestStatus.CANCELLED,
            List.of());

    private final MaintenanceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final MaintenanceCategoryRepository categoryRepository;
    private final MaintenanceRequestMapper requestMapper;

    @Override
    @Transactional
    public MaintenanceRequestResponse createRequest(CreateMaintenanceRequestRequest request) {
        User owner = getCurrentOwner();

        House house = getOwnedHouse(request.houseId(), owner.getId());
        Room room = getRoomInHouse(request.roomId(), house);
        Tenant tenant = getOwnedTenantOrNull(request.tenantId(), owner.getId());
        MaintenanceCategory category = getOwnedCategoryOrNull(request.categoryId(), owner.getId());

        MaintenanceRequest maintenanceRequest = requestMapper.toEntity(request);
        maintenanceRequest.setOwner(owner);
        maintenanceRequest.setHouse(house);
        maintenanceRequest.setRoom(room);
        maintenanceRequest.setTenant(tenant);
        maintenanceRequest.setCategory(category);
        maintenanceRequest.setRequestNo(generateRequestNo(owner.getId()));
        if (maintenanceRequest.getPriority() == null) {
            maintenanceRequest.setPriority(MaintenanceRequestPriority.MEDIUM);
        }
        maintenanceRequest.setStatus(MaintenanceRequestStatus.PENDING);

        return requestMapper.toResponse(requestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> getMyRequests() {
        User owner = getCurrentOwner();
        return requestRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(requestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse getRequestById(Long id) {
        return requestMapper.toResponse(getOwnedRequest(id));
    }

    @Override
    @Transactional
    public MaintenanceRequestResponse updateRequest(Long id, UpdateMaintenanceRequestRequest request) {
        MaintenanceRequest maintenanceRequest = getOwnedRequest(id);

        if (request.title() != null) {
            maintenanceRequest.setTitle(request.title());
        }
        if (request.description() != null) {
            maintenanceRequest.setDescription(request.description());
        }
        if (request.priority() != null) {
            maintenanceRequest.setPriority(request.priority());
        }

        return requestMapper.toResponse(requestRepository.save(maintenanceRequest));
    }

    @Override
    @Transactional
    public void changeStatus(Long id, ChangeMaintenanceRequestStatusRequest request) {
        MaintenanceRequest maintenanceRequest = getOwnedRequest(id);
        MaintenanceRequestStatus currentStatus = maintenanceRequest.getStatus();
        MaintenanceRequestStatus newStatus = request.status();

        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, List.of()).contains(newStatus)) {
            throw new InvalidMaintenanceOperationException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        maintenanceRequest.setStatus(newStatus);
        if (newStatus == MaintenanceRequestStatus.RESOLVED) {
            maintenanceRequest.setResolvedAt(LocalDateTime.now());
        }
        requestRepository.save(maintenanceRequest);
    }

    private MaintenanceRequest getOwnedRequest(Long id) {
        Long currentUserId = getCurrentOwner().getId();

        return requestRepository.findByIdAndOwnerId(id, currentUserId)
                .orElseThrow(() -> MaintenanceRequestNotFoundException.forId(id));
    }

    private House getOwnedHouse(Long houseId, Long ownerId) {
        return houseRepository.findByIdAndOwnerId(houseId, ownerId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }

    private Room getRoomInHouse(Long roomId, House house) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> RoomNotFoundException.forId(roomId));

        if (!room.getOwnerId().equals(house.getOwner().getId())) {
            throw new InvalidMaintenanceOperationException(
                    "Room " + roomId + " does not belong to this owner");
        }
        if (!room.getHouseId().equals(house.getId())) {
            throw new InvalidMaintenanceOperationException(
                    "Room " + roomId + " does not belong to house " + house.getId());
        }
        return room;
    }

    private Tenant getOwnedTenantOrNull(Long tenantId, Long ownerId) {
        if (tenantId == null) {
            return null;
        }
        return tenantRepository.findByIdAndOwnerId(tenantId, ownerId)
                .orElseThrow(() -> TenantNotFoundException.forId(tenantId));
    }

    private MaintenanceCategory getOwnedCategoryOrNull(Long categoryId, Long ownerId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndOwnerId(categoryId, ownerId)
                .orElseThrow(() -> MaintenanceCategoryNotFoundException.forId(categoryId));
    }

    private String generateRequestNo(Long ownerId) {
        long sequence = requestRepository.countByOwnerId(ownerId) + 1;
        String prefix = "MR-" + LocalDate.now().format(REQUEST_NO_DATE_FORMAT);

        String candidate = String.format("%s-%04d", prefix, sequence);
        while (requestRepository.existsByOwnerIdAndRequestNo(ownerId, candidate)) {
            sequence++;
            candidate = String.format("%s-%04d", prefix, sequence);
        }
        return candidate;
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
