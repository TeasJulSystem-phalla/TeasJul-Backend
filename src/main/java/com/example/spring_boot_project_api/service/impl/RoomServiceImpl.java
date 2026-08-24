package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoomNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomMapper;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.service.RoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRepuest request) {
        if (roomRepository.existsByHouseIdAndRoomNumber(request.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }
        Room room = roomMapper.toEntity(request);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        return roomMapper.toDto(getExistingRoom(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByHouse(Long houseId) {
        return roomRepository.findByHouseId(houseId).stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRepuest request) {
        Room existing = getExistingRoom(id);

        if (!existing.getRoomNumber().equals(request.getRoomNumber())
                && roomRepository.existsByHouseIdAndRoomNumber(existing.getHouseId(), request.getRoomNumber())) {
            throw new DuplicateResourceException(
                    "Room number already exists in this house: " + request.getRoomNumber());
        }

        existing.setOwnerId(request.getOwnerId());
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

        return roomMapper.toDto(roomRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        Room existing = getExistingRoom(id);
        roomRepository.delete(existing);
    }

    private Room getExistingRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> RoomNotFoundException.forId(id));
    }
}
