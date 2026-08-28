package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.RentalContract;

@Repository
public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {

    Optional<RentalContract> findByOwnerIdAndContractNo(Long ownerId, String contractNo);

    List<RentalContract> findByOwnerId(Long ownerId);

    List<RentalContract> findByOwnerIdAndRoomId(Long ownerId, Long roomId);

    List<RentalContract> findByOwnerIdAndStatus(Long ownerId, RentalContractStatus status);

    List<RentalContract> findByRoomId(Long roomId);

    List<RentalContract> findByStatus(RentalContractStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, RentalContractStatus status);

    @Query("""
        SELECT new com.example.spring_boot_project_api.dto.response.ActiveRoomTenantDto(
            rc.roomId,
            t.id,
            t.firstName,
            t.lastName,
            ct.isPrimary
        )
        FROM RentalContract rc, ContractTenant ct, Tenant t
        WHERE rc.id = ct.contractId
          AND ct.tenantId = t.id
          AND rc.roomId IN :roomIds
          AND rc.ownerId = :ownerId
          AND t.owner.id = :ownerId
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND ct.joinedAt <= :currentDate
          AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)
        ORDER BY ct.isPrimary DESC, ct.joinedAt ASC, ct.id ASC
    """)
    List<ActiveRoomTenantDto> findActiveTenantsByRoomIds(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("ownerId") Long ownerId,
            @Param("currentDate") LocalDate currentDate
    );
}
