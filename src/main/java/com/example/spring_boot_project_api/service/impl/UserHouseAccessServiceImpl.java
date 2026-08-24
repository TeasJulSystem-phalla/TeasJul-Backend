package com.example.spring_boot_project_api.service.impl;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.dto.request.GrantHouseAccessRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.dto.response.UserHouseAccessResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.HouseAccessNotFoundException;
import com.example.spring_boot_project_api.exception.HouseNotFoundException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.HouseMapper;
import com.example.spring_boot_project_api.mapper.UserHouseAccessMapper;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserHouseAccessId;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.UserHouseAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHouseAccessServiceImpl implements UserHouseAccessService {

    private final UserHouseAccessRepository userHouseAccessRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final HouseMapper houseMapper;
    private final UserHouseAccessMapper userHouseAccessMapper;

    @Override
    @Transactional
    public UserHouseAccessResponse grantAccess(Long houseId, GrantHouseAccessRequest request) {
        House house = getOwnedHouse(houseId);

        User targetUser = userRepository.findById(request.userId())
                .orElseThrow(() -> UserNotFoundException.forId(request.userId()));

        if (userHouseAccessRepository.existsByUserIdAndHouseId(targetUser.getId(), house.getId())) {
            throw new DuplicateResourceException("User " + targetUser.getUsername()
                    + " already has access to this house");
        }

        return userHouseAccessMapper.toResponse(
                userHouseAccessRepository.saveAndFlush(userHouseAccessMapper.toEntity(targetUser, house)));
    }

    @Override
    @Transactional
    public void revokeAccess(Long houseId, Long userId) {
        getOwnedHouse(houseId);

        if (!userHouseAccessRepository.existsByUserIdAndHouseId(userId, houseId)) {
            throw HouseAccessNotFoundException.forUserAndHouse(userId, houseId);
        }

        userHouseAccessRepository.deleteByUserIdAndHouseId(userId, houseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(Long userId, Long houseId) {
        return userHouseAccessRepository.existsByUserIdAndHouseId(userId, houseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserHouseAccessResponse> getHouseUsers(Long houseId) {
        getOwnedHouse(houseId);

        return userHouseAccessRepository.findByHouseId(houseId).stream()
                .map(userHouseAccessMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HouseResponse> getUserHouses() {
        Long currentUserId = getCurrentUser().getId();

        return userHouseAccessRepository.findByUserId(currentUserId).stream()
                .map(access -> houseMapper.toResponse(access.getHouse()))
                .toList();
    }

    private House getOwnedHouse(Long houseId) {
        Long currentUserId = getCurrentUser().getId();

        return houseRepository.findById(houseId)
                .map(house -> {
                    if (!house.getOwner().getId().equals(currentUserId)) {
                        throw new AccessDeniedException(
                                "You do not have permission to access this resource");
                    }
                    return house;
                })
                .orElseThrow(() -> HouseNotFoundException.forId(houseId));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        return userRepository.findByUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> UserNotFoundException.forId(null));
    }
}
