package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    List<UserRole> findByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
