package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.MaintenanceCost;

@Repository
public interface MaintenanceCostRepository extends JpaRepository<MaintenanceCost, Long> {

    List<MaintenanceCost> findAllByRequestIdOrderByCreatedAtAsc(Long requestId);

    Optional<MaintenanceCost> findByIdAndRequestId(Long id, Long requestId);
}
