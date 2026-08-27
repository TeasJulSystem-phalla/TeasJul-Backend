package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityReading;

@Repository
public interface UtilityReadingRepository
        extends JpaRepository<UtilityReading, Long>, JpaSpecificationExecutor<UtilityReading> {

    Optional<UtilityReading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);

    boolean existsByMeterIdAndReadingDate(Long meterId, LocalDate readingDate);
}
