package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.UtilityRate;

@Repository
public interface UtilityRateRepository
        extends JpaRepository<UtilityRate, Long>, JpaSpecificationExecutor<UtilityRate> {

    Optional<UtilityRate> findByIdAndOwnerId(Long id, Long ownerId);

    List<UtilityRate> findByOwnerIdAndHouseIdAndUtilityTypeId(Long ownerId, Long houseId, Long utilityTypeId);

    List<UtilityRate> findByOwnerIdAndHouseIdAndUtilityTypeIdAndIdNot(Long ownerId, Long houseId, Long utilityTypeId, Long id);
}
