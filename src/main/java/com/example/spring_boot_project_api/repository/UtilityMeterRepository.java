package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityMeter;

@Repository
public interface UtilityMeterRepository
        extends JpaRepository<UtilityMeter, Long>, JpaSpecificationExecutor<UtilityMeter> {

    Optional<UtilityMeter> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByMeterNumber(String meterNumber);

    boolean existsByMeterNumberAndIdNot(String meterNumber, Long id);
}
