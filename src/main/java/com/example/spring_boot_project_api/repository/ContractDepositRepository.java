package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.ContractDepositStatus;

import com.example.spring_boot_project_api.model.ContractDeposit;

@Repository
public interface ContractDepositRepository extends JpaRepository<ContractDeposit, Long> {

    List<ContractDeposit> findByRentalContractId(Long contractId);

    List<ContractDeposit> findByStatus(ContractDepositStatus status);
}
