package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;
import com.example.spring_boot_project_api.model.ContractTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ContractTenantRepository extends JpaRepository<ContractTenant, Long> {
    Optional<ContractTenant> findByContractIdAndTenantId(Long contractId, Long tenantId);

    List<ContractTenant> findByContractId(Long contractId);

    List<ContractTenant> findByTenantId(Long tenantId);

    boolean existsByContractIdAndTenantId(Long contractId, Long tenantId);
}
