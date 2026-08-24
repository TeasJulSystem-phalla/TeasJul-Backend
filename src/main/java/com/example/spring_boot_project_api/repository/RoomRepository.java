package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHouseId(Long houseId);

    List<Room> findByOwnerId(Long ownerId);

    List<Room> findByRoomTypeId(Long roomTypeId);

    Optional<Room> findByHouseIdAndRoomNumber(Long houseId, String roomNumber);

    boolean existsByHouseIdAndRoomNumber(Long houseId, String roomNumber);
}
