package com.example.spring_boot_project_api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.TenantInvitation;

@Repository
public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, Long> {

    Optional<TenantInvitation> findByTokenHash(String tokenHash);

    List<TenantInvitation> findAllByManagerIdOrderByCreatedAtDesc(Long managerId);

    Optional<TenantInvitation> findByIdAndManagerId(Long id, Long managerId);

    /**
     * Race-safe single-consumer claim: updates happen only when the invitation is
     * still PENDING and not yet expired. Exactly one concurrent caller wins.
     */
    @Modifying
    @Query("UPDATE TenantInvitation t SET t.status = USED, t.usedAt = :usedAt " +
            "WHERE t.id = :id AND t.status = PENDING AND t.expiresAt > :now")
    int markUsedIfPendingAndNotExpired(@Param("id") Long id,
            @Param("usedAt") LocalDateTime usedAt, @Param("now") LocalDateTime now);
}