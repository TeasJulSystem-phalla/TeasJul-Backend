package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.RoomUtility;

@Repository
public interface RoomUtilityRepository
        extends JpaRepository<RoomUtility, Long>, JpaSpecificationExecutor<RoomUtility> {

    List<RoomUtility> findByRoomIdAndStatus(Long roomId, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    List<RoomUtility> findByRoomIdInAndStatus(List<Long> roomIds, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    boolean existsByRoomIdAndUtilityTypeIdAndStatus(Long roomId, Long utilityTypeId, com.example.spring_boot_project_api.enums.UtilityTypeStatus status);

    void deleteByRoomIdAndUtilityTypeId(Long roomId, Long utilityTypeId);

    Optional<RoomUtility> findByRoomIdAndUtilityTypeId(Long roomId, Long utilityTypeId);
}
