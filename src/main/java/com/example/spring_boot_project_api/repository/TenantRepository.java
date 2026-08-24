package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
