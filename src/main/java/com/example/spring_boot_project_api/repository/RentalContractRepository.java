package com.example.spring_boot_project_api.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<RentalContract> findByOwnerIdInAndStatus(Collection<Long> ownerIds, RentalContractStatus status);

    List<RentalContract> findByRoomId(Long roomId);

    List<RentalContract> findByStatus(RentalContractStatus status);

    boolean existsByRoomIdAndStatus(Long roomId, RentalContractStatus status);

    @Query("SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds " +
            "ORDER BY rc.updatedAt DESC")
    List<RentalContract> findByRoomHouseIdIn(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT DISTINCT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId IN :houseIds AND rc.status = :status " +
            "ORDER BY rc.updatedAt DESC")
    List<RentalContract> findByRoomHouseIdInAndStatus(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RentalContractStatus status);

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
          AND rc.ownerId IN :ownerIds
          AND t.owner.id IN :ownerIds
          AND rc.status = com.example.spring_boot_project_api.enums.RentalContractStatus.ACTIVE
          AND ct.joinedAt <= :currentDate
          AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate)
        ORDER BY ct.isPrimary DESC, ct.joinedAt ASC, ct.id ASC
    """)
    List<ActiveRoomTenantDto> findActiveTenantsByRoomIds(
            @Param("roomIds") Collection<Long> roomIds,
            @Param("ownerIds") Collection<Long> ownerIds,
            @Param("currentDate") LocalDate currentDate
    );

    @Query("SELECT COUNT(rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.ownerId = :ownerId " +
            "AND rc.status = :status")
    long countByHouseIdAndOwnerIdAndStatus(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("status") RentalContractStatus status);

    @Query("SELECT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.ownerId = :ownerId " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(rc) FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId AND rc.status = :status")
    long countByHouseIdAndStatus(
            @Param("houseId") Long houseId,
            @Param("status") RentalContractStatus status);

    @Query("SELECT rc FROM RentalContract rc, Room r " +
            "WHERE rc.roomId = r.id AND r.houseId = :houseId " +
            "AND (:search IS NULL OR UPPER(rc.contractNo) LIKE UPPER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR rc.status = :status)")
    Page<RentalContract> findByHouseIdWithFilters(
            @Param("houseId") Long houseId,
            @Param("search") String search,
            @Param("status") RentalContractStatus status,
            Pageable pageable);
}
