package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.response.RoomAmenityResponse;
import com.example.spring_boot_project_api.exception.AmenityNotFoundException;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.RoomAmenityNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.RoomAmenityMapper;
import com.example.spring_boot_project_api.model.Amenity;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.AmenityRepository;
import com.example.spring_boot_project_api.repository.RoomAmenityRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.RoomAmenityService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomAmenityServiceImpl implements RoomAmenityService {

    private final RoomAmenityRepository roomAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomAmenityMapper roomAmenityMapper;
    private final HouseAccessService houseAccessService;

    @Override
    @Transactional
    public RoomAmenityResponse assignAmenityToRoom(Long roomId, Long amenityId) {
        Long currentUserId = getCurrentUser().getId();
        houseAccessService.resolveRoomHouseId(roomId);

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> AmenityNotFoundException.forId(amenityId));

        if (!amenity.getOwnerId().equals(currentUserId)
                && !houseAccessService.getScopedOwnerIds().contains(amenity.getOwnerId())) {
            throw new AccessDeniedException(
                    "You do not have permission to access this resource");
        }

        if (roomAmenityRepository.existsByIdRoomIdAndIdAmenityId(roomId, amenityId)) {
            throw new DuplicateResourceException(
                    "Amenity " + amenityId + " is already assigned to room " + roomId);
        }

        return roomAmenityMapper.toResponse(
                roomAmenityRepository.save(roomAmenityMapper.toEntity(roomId, amenity)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomAmenityResponse> getRoomAmenities(Long roomId) {
        houseAccessService.resolveRoomHouseId(roomId);
        return roomAmenityRepository.findByIdRoomId(roomId).stream()
                .map(roomAmenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeAmenityFromRoom(Long roomId, Long amenityId) {
        houseAccessService.resolveRoomHouseId(roomId);
        if (!roomAmenityRepository.existsByIdRoomIdAndIdAmenityId(roomId, amenityId)) {
            throw RoomAmenityNotFoundException.forIds(roomId, amenityId);
        }
        roomAmenityRepository.deleteByIdRoomIdAndIdAmenityId(roomId, amenityId);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
