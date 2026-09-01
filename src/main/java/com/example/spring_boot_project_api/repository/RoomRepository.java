package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHouseId(Long houseId);

    List<Room> findByHouseIdIn(List<Long> houseIds);

    List<Room> findByOwnerId(Long ownerId);

    List<Room> findByRoomTypeId(Long roomTypeId);

    Optional<Room> findByHouseIdAndRoomNumber(Long houseId, String roomNumber);

    boolean existsByHouseIdAndRoomNumber(Long houseId, String roomNumber);

    long countByHouseIdAndStatus(Long houseId, RoomStatus status);

    long countByHouseId(Long houseId);

    @Query("SELECT r FROM Room r WHERE r.houseId = :houseId AND r.ownerId = :ownerId " +
            "AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:roomTypeId IS NULL OR r.roomTypeId = :roomTypeId)")
    Page<Room> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("status") RoomStatus status,
            @Param("roomTypeId") Long roomTypeId,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.houseId = :houseId " +
            "AND (:search IS NULL OR UPPER(r.roomNumber) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(r.name) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:roomTypeId IS NULL OR r.roomTypeId = :roomTypeId)")
    Page<Room> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("search") String search,
            @Param("status") RoomStatus status,
            @Param("roomTypeId") Long roomTypeId,
            Pageable pageable);
}
