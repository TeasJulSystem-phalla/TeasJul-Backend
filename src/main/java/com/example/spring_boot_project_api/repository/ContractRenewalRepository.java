package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ContractRenewal;

@Repository
public interface ContractRenewalRepository extends JpaRepository<ContractRenewal, Long> {
    
    List<ContractRenewal> findByRentalContractIdOrderByRenewedAtDesc(Long contractId);
}
