package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.RentalContract;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {

    Optional<RentalContract> findByOwnerIdAndContractNo(Long ownerId, String contractNo);

    List<RentalContract> findByOwnerId(Long ownerId);

    List<RentalContract> findByRoomId(Long roomId);

    List<RentalContract> findByStatus(RentalContractStatus status);
}
