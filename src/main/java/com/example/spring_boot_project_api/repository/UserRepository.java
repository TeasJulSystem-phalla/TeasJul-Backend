package com.example.spring_boot_project_api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Searches users that can be linked to a new tenant profile, i.e. users that
     * do not already have a tenant profile. Searches across first/last/full name,
     * email, phone and username. Matching and ordering are done in the database.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
              AND NOT EXISTS (SELECT 1 FROM Tenant t WHERE t.user.id = u.id)
            """)
    Page<User> findAvailableForTenant(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(TRIM(u.firstName), ' ', TRIM(u.lastName))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<User> findAllWithFilters(@Param("q") String q, @Param("status") UserStatus status, Pageable pageable);

    @Query("SELECT u.status AS status, COUNT(u) AS cnt FROM User u GROUP BY u.status")
    java.util.List<Object[]> countGroupedByStatus();
}
