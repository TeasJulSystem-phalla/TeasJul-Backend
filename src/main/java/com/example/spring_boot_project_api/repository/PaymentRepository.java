package com.example.spring_boot_project_api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Payment> findByPaymentNo(String paymentNo);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByInvoiceIdAndStatusIn(Long invoiceId, List<PaymentStatus> statuses);

    List<Payment> findByInvoiceIdInAndStatus(List<Long> invoiceIds, PaymentStatus status);

    @Query("SELECT p.invoiceId, COALESCE(SUM(p.amount), 0) FROM Payment p "
            + "WHERE p.invoiceId IN :invoiceIds AND p.status = :status GROUP BY p.invoiceId")
    List<Object[]> sumPaidAmountByInvoiceIds(@Param("invoiceIds") List<Long> invoiceIds,
            @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoiceId = :invoiceId AND p.status = 'PAID'")
    BigDecimal findPaidTotalByInvoiceId(@Param("invoiceId") Long invoiceId);

    long count();
}
