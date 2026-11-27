package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ContractSequence;

import jakarta.persistence.LockModeType;

@Repository
public interface ContractSequenceRepository extends JpaRepository<ContractSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ContractSequence s WHERE s.sequenceName = :name")
    Optional<ContractSequence> findBySequenceNameWithLock(@Param("name") String name);
}
