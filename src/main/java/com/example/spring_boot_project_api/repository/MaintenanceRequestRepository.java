package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceRequest;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    Optional<MaintenanceRequest> findByIdAndOwnerId(Long id, Long ownerId);

    List<MaintenanceRequest> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByOwnerIdAndRequestNo(Long ownerId, String requestNo);

    long countByOwnerId(Long ownerId);
}
