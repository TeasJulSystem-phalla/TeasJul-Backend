package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.HouseService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {

    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final HouseMapper houseMapper;

    @Override
    @Transactional
    public HouseResponse createHouse(CreateHouseRequest request) {
        User owner = getCurrentOwner();

        validateUniqueFields(owner.getId(), request.name(), request.slug(), request.code(), null);

        House house = houseMapper.toEntity(request);
        house.setOwner(owner);

        return houseMapper.toResponse(houseRepository.save(house));
    }

    @Override
    @Transactional(readOnly = true)
    public HouseResponse getHouseById(Long id) {
        return houseMapper.toResponse(getOwnedHouse(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getMyHouses() {
        User owner = getCurrentOwner();
        return houseRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(houseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public HouseResponse updateHouse(Long id, UpdateHouseRequest request) {
        House house = getOwnedHouse(id);
        Long ownerId = house.getOwner().getId();

        validateUniqueFields(ownerId, request.name(), request.slug(), request.code(), house.getId());

        if (request.name() != null) {
            house.setName(request.name());
        }
        if (request.slug() != null) {
            house.setSlug(request.slug());
        }
        if (request.code() != null) {
            house.setCode(request.code());
        }
        if (request.address() != null) {
            house.setAddress(request.address());
        }
        if (request.postalCode() != null) {
            house.setPostalCode(request.postalCode());
        }
        if (request.latitude() != null) {
            house.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            house.setLongitude(request.longitude());
        }
        if (request.totalRooms() != null) {
            house.setTotalRooms(request.totalRooms());
        }
        if (request.availableRooms() != null) {
            house.setAvailableRooms(request.availableRooms());
        }
        if (request.description() != null) {
            house.setDescription(request.description());
        }
        if (request.status() != null) {
            house.setStatus(request.status());
        }

        return houseMapper.toResponse(houseRepository.save(house));
    }

    @Override
    @Transactional
    public void deleteHouse(Long id) {
        House house = getOwnedHouse(id);
        houseRepository.delete(house);
    }

    private House getOwnedHouse(Long id) {
        Long currentUserId = getCurrentOwner().getId();

        return houseRepository.findById(id)
                .map(house -> {
                    if (!house.getOwner().getId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return house;
                })
                .orElseThrow(() -> HouseNotFoundException.forId(id));
    }

    private void validateUniqueFields(Long ownerId, String name, String slug, String code, Long houseId) {
        boolean isCreate = houseId == null;

        if (name != null && (isCreate
                ? houseRepository.existsByOwnerIdAndName(ownerId, name)
                : houseRepository.existsByOwnerIdAndNameAndIdNot(ownerId, name, houseId))) {
            throw new DuplicateResourceException("House name already exists for this owner: " + name);
        }
        if (slug != null && (isCreate
                ? houseRepository.existsByOwnerIdAndSlug(ownerId, slug)
                : houseRepository.existsByOwnerIdAndSlugAndIdNot(ownerId, slug, houseId))) {
            throw new DuplicateResourceException("House slug already exists for this owner: " + slug);
        }
        if (code != null && (isCreate
                ? houseRepository.existsByOwnerIdAndCode(ownerId, code)
                : houseRepository.existsByOwnerIdAndCodeAndIdNot(ownerId, code, houseId))) {
            throw new DuplicateResourceException("House code already exists for this owner: " + code);
        }
    }

    private User getCurrentOwner() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
