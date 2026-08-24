package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.model.UserHouseAccessId;

@Repository
public interface UserHouseAccessRepository extends JpaRepository<UserHouseAccess, UserHouseAccessId> {

    boolean existsByUserIdAndHouseId(Long userId, Long houseId);

    List<UserHouseAccess> findByUserId(Long userId);

    List<UserHouseAccess> findByHouseId(Long houseId);

    void deleteByUserIdAndHouseId(Long userId, Long houseId);
}
