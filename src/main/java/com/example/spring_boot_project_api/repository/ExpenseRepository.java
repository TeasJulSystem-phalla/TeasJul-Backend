package com.example.spring_boot_project_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.model.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Expense> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndExpenseNo(Long ownerId, String expenseNo);

    boolean existsByOwnerIdAndExpenseNoAndIdNot(Long ownerId, String expenseNo, Long id);
}
