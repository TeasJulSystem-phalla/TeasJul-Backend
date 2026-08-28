package com.example.spring_boot_project_api.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.dto.response.RoomTenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.RoomService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RoomMapper roomMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRepuest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyHouseOwnership(request.getHouseId(), currentUserId);

        if (roomRepository.existsByHouseIdAndRoomNumber(request.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }
        Room room = roomMapper.toEntity(request);
        room.setOwnerId(currentUserId);
        return roomMapper.toDto(roomRepository.save(room), null);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        Room room = getOwnedRoom(id);
        RoomTenantResponse tenant = getActiveTenantForRoom(room.getId(), currentUserId);
        return roomMapper.toDto(room, tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<Room> rooms = roomRepository.findByOwnerId(currentUserId);
        Map<Long, RoomTenantResponse> tenantMap = getActiveTenantsForRooms(rooms, currentUserId);
        return rooms.stream()
                .map(room -> roomMapper.toDto(room, tenantMap.get(room.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHouse(Long houseId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyHouseOwnership(houseId, currentUserId);
        List<Room> rooms = roomRepository.findByHouseId(houseId);
        Map<Long, RoomTenantResponse> tenantMap = getActiveTenantsForRooms(rooms, currentUserId);
        return rooms.stream()
                .map(room -> roomMapper.toDto(room, tenantMap.get(room.getId())))
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRepuest request) {
        Room existing = getOwnedRoom(id);
        Long currentUserId = securityUtils.getCurrentUserId();
        verifyHouseOwnership(request.getHouseId(), currentUserId);

        if (!existing.getRoomNumber().equals(request.getRoomNumber())
                && roomRepository.existsByHouseIdAndRoomNumber(request.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }

        existing.setHouseId(request.getHouseId());
        existing.setRoomTypeId(request.getRoomTypeId());
        existing.setRoomNumber(request.getRoomNumber());
        existing.setName(request.getName());
        existing.setFloorNumber(request.getFloorNumber());
        if (request.getMonthlyRent() != null) {
            existing.setMonthlyRent(request.getMonthlyRent());
        }
        if (request.getSecurityDeposit() != null) {
            existing.setSecurityDeposit(request.getSecurityDeposit());
        }
        if (request.getCapacity() != null) {
            existing.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        existing.setDescription(request.getDescription());

        Room saved = roomRepository.save(existing);
        RoomTenantResponse tenant = getActiveTenantForRoom(saved.getId(), currentUserId);
        return roomMapper.toDto(saved, tenant);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room existing = getOwnedRoom(id);
        roomRepository.delete(existing);
    }

    private Map<Long, RoomTenantResponse> getActiveTenantsForRooms(List<Room> rooms, Long ownerId) {
        if (rooms == null || rooms.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        List<ActiveRoomTenantDto> activeTenants = rentalContractRepository.findActiveTenantsByRoomIds(
                roomIds, ownerId, LocalDate.now()
        );
        Map<Long, RoomTenantResponse> map = new HashMap<>();
        for (ActiveRoomTenantDto item : activeTenants) {
            map.putIfAbsent(item.roomId(), new RoomTenantResponse(item.tenantId(), item.firstName(), item.lastName()));
        }
        return map;
    }

    private RoomTenantResponse getActiveTenantForRoom(Long roomId, Long ownerId) {
        List<ActiveRoomTenantDto> list = rentalContractRepository.findActiveTenantsByRoomIds(
                List.of(roomId), ownerId, LocalDate.now()
        );
        if (list.isEmpty()) {
            return null;
        }
        ActiveRoomTenantDto item = list.get(0);
        return new RoomTenantResponse(item.tenantId(), item.firstName(), item.lastName());
    }

    private Room getOwnedRoom(Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return roomRepository.findById(id)
                .map(room -> {
                    if (!room.getOwnerId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return room;
                })
                .orElseThrow(() -> RoomNotFoundException.forId(id));
    }

    private void verifyHouseOwnership(Long houseId, Long currentUserId) {
        houseRepository.findByIdAndOwnerId(houseId, currentUserId)
                .map(House::getId)
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }
}
