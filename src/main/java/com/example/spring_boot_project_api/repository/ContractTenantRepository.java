package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.example.spring_boot_project_api.model.ContractTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractTenantRepository extends JpaRepository<ContractTenant, Long> {
    Optional<ContractTenant> findByContractIdAndTenantId(Long contractId, Long tenantId);

    List<ContractTenant> findByContractId(Long contractId);

    List<ContractTenant> findByTenantId(Long tenantId);

    boolean existsByContractIdAndTenantId(Long contractId, Long tenantId);

    @Query("""
        SELECT COUNT(ct) > 0
        FROM ContractTenant ct, RentalContract rc
        WHERE ct.contractId = rc.id
          AND ct.tenantId = :tenantId
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)
    """)
    boolean isTenantActivelyRenting(
            @Param("tenantId") Long tenantId,
            @Param("currentDate") LocalDate currentDate
    );
}
