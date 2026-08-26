package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.RoomTypeRequest;
import com.example.spring_boot_project_api.dto.response.RoomTypeResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoomTypeNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomTypeMapper;
import com.example.spring_boot_project_api.model.RoomType;
import com.example.spring_boot_project_api.repository.RoomTypeRepository;
import com.example.spring_boot_project_api.service.RoomTypeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;

    @Override
    @Transactional
    public RoomTypeResponse createRoomType(RoomTypeRequest request) {
        if (roomTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Room type code already exists: " + request.getCode());
        }
        RoomType roomType = roomTypeMapper.toEntity(request);
        return roomTypeMapper.toDto(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomTypeById(Long id) {
        return roomTypeMapper.toDto(getExistingRoomType(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getAllRoomTypes() {
        return roomTypeRepository.findAll().stream()
                .map(roomTypeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getRoomTypesByOwner(Long ownerId) {
        return roomTypeRepository.findByOwnerId(ownerId).stream()
                .map(roomTypeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request) {
        RoomType existing = getExistingRoomType(id);

        if (!existing.getCode().equals(request.getCode())
                && roomTypeRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Room type code already exists: " + request.getCode());
        }

        existing.setOwnerId(request.getOwnerId());
        existing.setName(request.getName());
        existing.setCode(request.getCode());
        existing.setDescription(request.getDescription());
        if (request.getCapacity() != null) {
            existing.setCapacity(request.getCapacity());
        }
        if (request.getDefaultRent() != null) {
            existing.setDefaultRent(request.getDefaultRent());
        }
        if (request.getDefaultDeposit() != null) {
            existing.setDefaultDeposit(request.getDefaultDeposit());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        return roomTypeMapper.toDto(roomTypeRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteRoomType(Long id) {
        RoomType existing = getExistingRoomType(id);
        roomTypeRepository.delete(existing);
    }

    private RoomType getExistingRoomType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> RoomTypeNotFoundException.forId(id));
    }
}
