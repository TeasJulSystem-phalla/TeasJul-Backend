package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.UtilityRateStatus;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.UtilityRate;
import com.example.spring_boot_project_api.model.UtilityReading;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = "items")
    List<Invoice> findByContractIdIn(List<Long> contractIds);

    @EntityGraph(attributePaths = "items")
    Page<Invoice> findByContractIdIn(List<Long> contractIds, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Invoice> findByIdAndContractIdIn(Long id, List<Long> contractIds);

    List<Invoice> findByContractIdInAndStatusIn(List<Long> contractIds, List<InvoiceStatus> statuses);

    long countByContractIdInAndStatus(List<Long> contractIds, InvoiceStatus status);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.discount, 0)), 0)
        FROM Invoice i
        WHERE i.contractId IN :contractIds AND i.status = :status
    """)
    BigDecimal sumTotalByContractIdInAndStatus(@Param("contractIds") List<Long> contractIds,
            @Param("status") InvoiceStatus status);

    boolean existsByContractIdAndBillingYearAndBillingMonth(Long contractId, Integer billingYear, Integer billingMonth);

    Optional<Invoice> findByContractIdAndBillingYearAndBillingMonth(Long contractId, Integer billingYear, Integer billingMonth);

    // The rate effective for the given date: the latest ACTIVE rate whose
    // effective_from <= date and (effective_to is null or >= date).
    @Query("""
        SELECT r FROM UtilityRate r
        WHERE r.owner.id = :ownerId
          AND r.houseId = :houseId
          AND r.utilityTypeId = :utilityTypeId
          AND r.status = :status
          AND r.effectiveFrom <= :date
          AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date)
        ORDER BY r.effectiveFrom DESC, r.id DESC
    """)
    List<UtilityRate> findEffectiveRate(@Param("ownerId") Long ownerId,
            @Param("houseId") Long houseId,
            @Param("utilityTypeId") Long utilityTypeId,
            @Param("date") LocalDate date,
            @Param("status") UtilityRateStatus status);

    // The meter's reading for a given billing period: the latest reading on or
    // before the first day of the billing month (used as the baseline 'current'
    // for usage). Actual charge uses the reading dated within the month.
    @Query("""
        SELECT rd FROM UtilityReading rd
        WHERE rd.meter.id = :meterId
          AND rd.readingDate <= :endOfMonth
        ORDER BY rd.readingDate DESC, rd.id DESC
    """)
    List<UtilityReading> findReadingsForPeriod(@Param("meterId") Long meterId,
            @Param("endOfMonth") LocalDate endOfMonth);
}
