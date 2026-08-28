package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.PaymentMethod;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<PaymentMethod> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndCode(Long ownerId, String code);

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}
