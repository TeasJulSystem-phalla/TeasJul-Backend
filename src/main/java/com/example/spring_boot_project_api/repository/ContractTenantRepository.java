package com.example.spring_boot_project_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.ContractTenant;

@Repository
public interface ContractTenantRepository extends JpaRepository<ContractTenant, Long> {

    List<ContractTenant> findByRentalContractId(Long contractId);

    List<ContractTenant> findByTenantId(Long tenantId);

    boolean existsByRentalContractIdAndTenantId(Long contractId, Long tenantId);
}
