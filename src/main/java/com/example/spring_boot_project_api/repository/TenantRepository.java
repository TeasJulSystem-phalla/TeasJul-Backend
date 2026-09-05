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

import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.model.Tenant;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByIdAndOwnerId(Long id, Long ownerId);

    List<Tenant> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Tenant> findByUserId(Long userId);

    boolean existsByOwnerIdAndTenantCode(Long ownerId, String tenantCode);

    boolean existsByOwnerIdAndPhone(Long ownerId, String phone);

    boolean existsByOwnerIdAndTenantCodeAndIdNot(Long ownerId, String tenantCode, Long id);

    boolean existsByOwnerIdAndPhoneAndIdNot(Long ownerId, String phone, Long id);

    @Query("SELECT DISTINCT t FROM Tenant t, ContractTenant ct, RentalContract rc, Room r " +
            "WHERE t.id = ct.tenantId AND ct.contractId = rc.id AND rc.roomId = r.id " +
            "AND r.houseId = :houseId AND t.owner.id = :ownerId " +
            "AND rc.status = :status AND ct.joinedAt <= :currentDate " +
            "AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate) " +
            "AND (:search IS NULL OR UPPER(t.firstName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.lastName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.phone) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.tenantCode) LIKE UPPER(CONCAT('%', :search, '%')))")
    Page<Tenant> findActiveTenantsByHouseId(
            @Param("houseId") Long houseId,
            @Param("ownerId") Long ownerId,
            @Param("status") RentalContractStatus status,
            @Param("currentDate") LocalDate currentDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT DISTINCT t FROM Tenant t, ContractTenant ct, RentalContract rc, Room r " +
            "WHERE t.id = ct.tenantId AND ct.contractId = rc.id AND rc.roomId = r.id " +
            "AND r.houseId = :houseId " +
            "AND rc.status = :status AND ct.joinedAt <= :currentDate " +
            "AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate) " +
            "AND (:search IS NULL OR UPPER(t.firstName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.lastName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.phone) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.tenantCode) LIKE UPPER(CONCAT('%', :search, '%')))")
    Page<Tenant> findActiveTenantsByHouseId(
            @Param("houseId") Long houseId,
            @Param("status") RentalContractStatus status,
            @Param("currentDate") LocalDate currentDate,
            @Param("search") String search,
            Pageable pageable);
    @Query("SELECT DISTINCT t FROM Tenant t, ContractTenant ct, RentalContract rc, Room r " +
            "WHERE t.id = ct.tenantId AND ct.contractId = rc.id AND rc.roomId = r.id " +
            "AND r.houseId IN :houseIds " +
            "AND rc.status = :status AND ct.joinedAt <= :currentDate " +
            "AND (ct.leftAt IS NULL OR ct.leftAt >= :currentDate) " +
            "AND (:search IS NULL OR UPPER(t.firstName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.lastName) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.phone) LIKE UPPER(CONCAT('%', :search, '%')) " +
            "OR UPPER(t.tenantCode) LIKE UPPER(CONCAT('%', :search, '%')))")
    Page<Tenant> findActiveTenantsByHouseIds(
            @Param("houseIds") Collection<Long> houseIds,
            @Param("status") RentalContractStatus status,
            @Param("currentDate") LocalDate currentDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT DISTINCT t FROM Tenant t WHERE t.house.id IN :houseIds ORDER BY t.createdAt DESC")
    List<Tenant> findAllByHouseIdsOrderByCreatedAtDesc(@Param("houseIds") Collection<Long> houseIds);

    @Query("SELECT t FROM Tenant t WHERE t.invitedBy.id = :managerId " +
            "OR (t.owner.id = :managerId AND t.invitedBy IS NULL) " +
            "ORDER BY t.createdAt DESC")
    List<Tenant> findAllVisibleToManagerOrderByCreatedAtDesc(@Param("managerId") Long managerId);
}
