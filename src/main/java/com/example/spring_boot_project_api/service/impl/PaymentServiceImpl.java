package com.example.spring_boot_project_api.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.config.PayWayConfig;
import com.example.spring_boot_project_api.dto.request.CreatePaymentRequest;
import com.example.spring_boot_project_api.dto.request.RecordManualPaymentRequest;
import com.example.spring_boot_project_api.dto.response.PageResponse;
import com.example.spring_boot_project_api.dto.response.PaymentListResponse;
import com.example.spring_boot_project_api.dto.response.PaymentResponse;
import com.example.spring_boot_project_api.dto.response.PaymentStatusResponse;
import com.example.spring_boot_project_api.dto.response.PayWayCheckTransactionResponse;
import com.example.spring_boot_project_api.dto.response.PayWayQrResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentMethodStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.exception.InvalidPaymentOperationException;
import com.example.spring_boot_project_api.exception.InvoiceNotFoundException;
import com.example.spring_boot_project_api.exception.PaymentMethodNotFoundException;
import com.example.spring_boot_project_api.exception.PaymentNotFoundException;
import com.example.spring_boot_project_api.exception.PayWayIntegrationException;
import com.example.spring_boot_project_api.mapper.PaymentMapper;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.PaymentMethodRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;
import com.example.spring_boot_project_api.service.PayWayService;
import com.example.spring_boot_project_api.service.PaymentService;
import com.example.spring_boot_project_api.util.SecurityUtils;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final int QR_LIFETIME_MINUTES = 15;
    private static final int PAYMENT_EXPIRY_MINUTES = 15;
    private static final int PAYWAY_APPROVED_STATUS = 0;
    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES =
            List.of(PaymentStatus.PENDING);
    private static final List<PaymentStatus> RECONCILABLE_PAYMENT_STATUSES =
            List.of(PaymentStatus.PENDING, PaymentStatus.EXPIRED);

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMapper paymentMapper;
    private final PayWayService payWayService;
    private final HouseAccessService houseAccessService;
    private final SecurityUtils securityUtils;
    private final PayWayConfig payWayConfig;
    private final TenantContextResolver tenantContextResolver;
    private final ContractTenantRepository contractTenantRepository;
    private final RentalContractRepository rentalContractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        verifyCanCreatePayment();

        Invoice invoice = loadAndAuthorizeInvoice(request.invoiceId());
        PaymentMethod paymentMethod = loadAndValidatePaymentMethod(request.paymentMethodId(), invoice);

        validateInvoiceForPayment(invoice);
        expireStalePayments(invoice.getId());
        validateNoActivePayment(invoice.getId());

        BigDecimal amount = calculateOutstandingAmount(invoice);

        String paymentNo = generatePaymentNo();
        String transactionId = generateTransactionId();

        Payment payment = Payment.builder()
                .owner(invoice.getOwner())
                .invoiceId(invoice.getId())
                .paymentMethod(paymentMethod)
                .paymentNo(paymentNo)
                .transactionId(transactionId)
                .amount(amount)
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created payment: id={}, paymentNo={}, tranId={}", savedPayment.getId(), paymentNo, transactionId);

        try {
            PayWayQrResponse qrResponse = payWayService.generateQr(
                    transactionId,
                    amount,
                    "USD",
                    payWayConfig.getCallbackUrl(),
                    QR_LIFETIME_MINUTES);

            savedPayment.setQrCode(qrResponse.qrString());
            Payment updatedPayment = paymentRepository.save(savedPayment);

            log.info("PayWay QR generated: paymentId={}, tranId={}", updatedPayment.getId(), transactionId);
            return paymentMapper.toResponse(updatedPayment);

        } catch (PayWayIntegrationException e) {
            savedPayment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(savedPayment);
            log.error("PayWay QR generation failed for payment: id={}, tranId={}", savedPayment.getId(), transactionId);
            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentResponse recordManualPayment(RecordManualPaymentRequest request) {
        verifyCanRecordManualPayment();

        Invoice invoice = loadAndAuthorizeInvoice(request.invoiceId());
        PaymentMethod paymentMethod = loadAndValidatePaymentMethod(request.paymentMethodId(), invoice);

        validateInvoiceForPayment(invoice);

        BigDecimal alreadyPaid = paymentRepository.findPaidTotalByInvoiceId(invoice.getId());
        if (alreadyPaid == null) {
            alreadyPaid = BigDecimal.ZERO;
        }
        BigDecimal outstanding = invoice.getTotalAmount()
                .subtract(invoice.getDiscount())
                .subtract(alreadyPaid);
        if (outstanding.signum() <= 0) {
            throw new InvalidPaymentOperationException("This invoice has no outstanding amount to pay");
        }

        BigDecimal amount = request.amount() != null ? request.amount() : outstanding;
        if (amount.signum() <= 0) {
            throw new InvalidPaymentOperationException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new InvalidPaymentOperationException(
                    "Payment amount exceeds the outstanding balance (remaining: " + outstanding + ")");
        }

        String paymentNo = generatePaymentNo();
        String transactionId = generateTransactionId();
        Payment payment = Payment.builder()
                .owner(invoice.getOwner())
                .invoiceId(invoice.getId())
                .paymentMethod(paymentMethod)
                .paymentNo(paymentNo)
                .transactionId(transactionId)
                .amount(amount)
                .currency("USD")
                .status(PaymentStatus.PAID)
                .paidAt(request.paidAt() != null ? request.paidAt() : LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Recorded manual payment: paymentId={}, paymentNo={}, invoiceId={}, amount={}, method={}",
                savedPayment.getId(), paymentNo, invoice.getId(), amount, paymentMethod.getName());

        updateInvoiceStatus(invoice.getId());

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void processCallback(Map<String, Object> callback, HttpServletRequest request) {
        if (callback == null || callback.isEmpty()) {
            log.warn("Callback with empty payload, ignoring");
            return;
        }

        String tranId = (String) callback.get("tran_id");
        if (tranId == null || tranId.isBlank()) {
            log.warn("Callback missing tran_id, ignoring");
            return;
        }

        log.info("ABA callback received: tranId={}, callbackStatus={}",
                tranId, callback.get("status"));

        // Signature verification is defense-in-depth: the authoritative check is the
        // server-to-server Check Transaction call (HMAC-authenticated with our API key).
        // A failed signature never silently drops a real payment notification.
        boolean signatureValid = payWayService.verifyCallbackSignature(callback, request);
        if (!signatureValid) {
            log.warn("Invalid callback signature, reconciling via Check Transaction anyway: tranId={}", tranId);
        }

        Payment payment = paymentRepository.findByTransactionId(tranId)
                .orElse(null);

        if (payment == null) {
            log.warn("Callback for unknown transaction: tranId={}, ignoring", tranId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Idempotent: payment already PAID, ignoring: paymentId={}, tranId={}", payment.getId(), tranId);
            return;
        }

        if (!RECONCILABLE_PAYMENT_STATUSES.contains(payment.getStatus())) {
            log.info("Ignoring callback for non-reconcilable payment: paymentId={}, status={}",
                    payment.getId(), payment.getStatus());
            return;
        }

        reconcileWithPayWay(payment, tranId);
    }

    @Override
    @Transactional
    public PaymentStatusResponse getPaymentStatus(String transactionId) {
        Payment payment = loadAndAuthorizePayment(transactionId);

        // Reconcile non-terminal payments against ABA before returning the status,
        // so the frontend gets an authoritative result even if the ABA callback
        // delivery was delayed or lost.
        if (RECONCILABLE_PAYMENT_STATUSES.contains(payment.getStatus())) {
            reconcileWithPayWay(payment, payment.getTransactionId());
        }

        return toStatusResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentListResponse> getPayments(Long houseId, PaymentStatus status,
            LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<Long> ownerIds = houseAccessService.getScopedOwnerIds();
        if (ownerIds.isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        }

        List<Long> accessibleHouseIds = houseAccessService.getAccessibleHouseIds();
        if (houseId != null) {
            houseAccessService.requireHouseAccess(houseId);
            accessibleHouseIds = List.of(houseId);
        }
        final List<Long> scopeHouseIds = accessibleHouseIds;

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("owner").get("id").in(ownerIds));

            if (!scopeHouseIds.isEmpty()) {
                Subquery<Long> roomSub = query.subquery(Long.class);
                Root<Room> roomRoot = roomSub.from(Room.class);
                roomSub.select(roomRoot.get("id"))
                        .where(roomRoot.get("houseId").in(scopeHouseIds));

                Subquery<Long> contractSub = query.subquery(Long.class);
                Root<RentalContract> contractRoot = contractSub.from(RentalContract.class);
                contractSub.select(contractRoot.get("id"))
                        .where(contractRoot.get("roomId").in(roomSub));

                Subquery<Long> invoiceSub = query.subquery(Long.class);
                Root<Invoice> invoiceRoot = invoiceSub.from(Invoice.class);
                invoiceSub.select(invoiceRoot.get("id"))
                        .where(invoiceRoot.get("contractId").in(contractSub));

                predicates.add(root.get("invoiceId").in(invoiceSub));
            }

            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> page = paymentRepository.findAll(spec, pageable);

        List<PaymentListResponse> content = page.getContent().stream()
                .map(this::toListResponse)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private PaymentListResponse toListResponse(Payment payment) {
        Long houseId = null;
        String houseName = null;
        String roomNumber = null;
        String invoiceNo = null;
        Long tenantId = null;
        String tenantName = null;

        Invoice invoice = payment.getInvoiceId() != null
                ? invoiceRepository.findById(payment.getInvoiceId()).orElse(null)
                : null;
        if (invoice != null) {
            invoiceNo = invoice.getInvoiceNo();
            RentalContract contract = rentalContractRepository.findById(invoice.getContractId()).orElse(null);
            if (contract != null) {
                Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                if (room != null) {
                    houseId = room.getHouseId();
                    roomNumber = room.getRoomNumber();
                    House house = houseRepository.findById(room.getHouseId()).orElse(null);
                    if (house != null) {
                        houseName = house.getName();
                    }
                }
                Tenant tenant = primaryTenantOf(invoice.getContractId());
                if (tenant != null) {
                    tenantId = tenant.getId();
                    String first = tenant.getFirstName() != null ? tenant.getFirstName() : "";
                    String last = tenant.getLastName() != null ? tenant.getLastName() : "";
                    tenantName = (first + " " + last).trim();
                    if (tenantName.isEmpty()) {
                        tenantName = tenant.getEmail();
                    }
                }
            }
        }

        PaymentMethod method = payment.getPaymentMethod();

        return new PaymentListResponse(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getTransactionId(),
                payment.getInvoiceId(),
                invoiceNo,
                method != null ? method.getId() : null,
                method != null ? method.getName() : null,
                houseId,
                houseName,
                tenantId,
                tenantName,
                roomNumber,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }

    private Tenant primaryTenantOf(Long contractId) {
        List<ContractTenant> links = contractTenantRepository.findByContractId(contractId);
        ContractTenant chosen = null;
        for (ContractTenant link : links) {
            if (Boolean.TRUE.equals(link.getIsPrimary())) {
                chosen = link;
                break;
            }
        }
        if (chosen == null && !links.isEmpty()) {
            chosen = links.stream()
                    .min((a, b) -> a.getJoinedAt().compareTo(b.getJoinedAt()))
                    .orElse(null);
        }
        if (chosen == null) return null;
        return tenantRepository.findById(chosen.getTenantId()).orElse(null);
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        String invoiceNo = null;
        InvoiceStatus invoiceStatus = null;
        if (payment.getInvoiceId() != null) {
            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                    .orElse(null);
            if (invoice != null) {
                invoiceNo = invoice.getInvoiceNo();
                invoiceStatus = invoice.getStatus();
            }
        }
        return new PaymentStatusResponse(
                payment.getTransactionId(),
                payment.getId(),
                payment.getInvoiceId(),
                invoiceNo,
                payment.getStatus(),
                invoiceStatus,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt());
    }

    private void reconcileWithPayWay(Payment payment, String tranId) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        PayWayCheckTransactionResponse checkResult;
        try {
            log.info("Verifying payment with ABA: tranId={}", tranId);
            checkResult = payWayService.checkTransaction(tranId);
        } catch (PayWayIntegrationException e) {
            log.error("ABA Check Transaction failed for payment: paymentId={}, tranId={}",
                    payment.getId(), tranId);
            return;
        }

        log.info("ABA payment verification result: tranId={}, paymentStatus={}",
                tranId,
                checkResult.data() != null ? checkResult.data().paymentStatus() : "unknown");

        PayWayCheckTransactionResponse.TransactionData txData = checkResult.data();
        if (txData == null) {
            log.warn("ABA Check Transaction returned no data: tranId={}", tranId);
            return;
        }

        if (!verifyTransactionId(tranId, checkResult)) {
            return;
        }

        if (!verifyAmount(payment, txData)) {
            return;
        }

        if (!verifyCurrency(payment, txData)) {
            return;
        }

        if (!verifyPaymentApproved(txData)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment not approved by ABA: paymentId={}, tranId={}, status={}",
                    payment.getId(), tranId, txData.paymentStatus());
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("ABA confirmed payment: paymentId={}, tranId={}, amount={}, currency={}",
                payment.getId(), tranId, payment.getAmount(), payment.getCurrency());

        updateInvoiceStatus(payment.getInvoiceId());
    }

    private boolean verifyTransactionId(String localTranId, PayWayCheckTransactionResponse checkResult) {
        String paywayTranId = checkResult.status().tranId();
        if (paywayTranId != null && !localTranId.equals(paywayTranId)) {
            log.error("Transaction ID mismatch: local={}, payway={}", localTranId, paywayTranId);
            return false;
        }
        return true;
    }

    private boolean verifyAmount(Payment payment, PayWayCheckTransactionResponse.TransactionData txData) {
        if (txData.paymentAmount() == null) {
            log.error("Check Transaction returned null amount for tranId={}", payment.getTransactionId());
            return false;
        }
        BigDecimal paywayAmount = BigDecimal.valueOf(txData.paymentAmount());
        if (paywayAmount.compareTo(payment.getAmount()) != 0) {
            log.error("Amount mismatch: local={}, payway={}, tranId={}",
                    payment.getAmount(), paywayAmount, payment.getTransactionId());
            return false;
        }
        return true;
    }

    private boolean verifyCurrency(Payment payment, PayWayCheckTransactionResponse.TransactionData txData) {
        if (txData.paymentCurrency() == null) {
            log.error("Check Transaction returned null currency for tranId={}", payment.getTransactionId());
            return false;
        }
        if (!payment.getCurrency().equals(txData.paymentCurrency())) {
            log.error("Currency mismatch: local={}, payway={}, tranId={}",
                    payment.getCurrency(), txData.paymentCurrency(), payment.getTransactionId());
            return false;
        }
        return true;
    }

    private boolean verifyPaymentApproved(PayWayCheckTransactionResponse.TransactionData txData) {
        return txData.paymentStatusCode() != null && txData.paymentStatusCode() == PAYWAY_APPROVED_STATUS;
    }

    @Transactional
    void updateInvoiceStatus(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.error("Invoice not found during callback processing: invoiceId={}", invoiceId);
            return;
        }

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            return;
        }

        BigDecimal paidTotal = paymentRepository.findPaidTotalByInvoiceId(invoiceId);
        if (paidTotal == null) {
            paidTotal = BigDecimal.ZERO;
        }

        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getDiscount());

        if (paidTotal.compareTo(outstanding) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            log.info("Invoice marked as PAID: invoiceId={}, paid={}, outstanding={}", invoiceId, paidTotal, outstanding);
        } else if (paidTotal.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIAL);
            invoiceRepository.save(invoice);
            log.info("Invoice marked as PARTIAL: invoiceId={}, paid={}, outstanding={}", invoiceId, paidTotal, outstanding);
        }
    }

    private Payment loadAndAuthorizePayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> PaymentNotFoundException.forTransactionId(transactionId));

        Long currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.hasRole(SecurityUtils.ROLE_TENANT)
                && !securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
            if (invoice == null) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this resource");
            }
            Tenant tenant = tenantContextResolver.linkedTenantOf(currentUserId);
            if (tenant == null
                    || !contractTenantRepository.existsByContractIdAndTenantId(
                            invoice.getContractId(), tenant.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this resource");
            }
        } else {
            Long invoiceHouseId = houseAccessService.resolveInvoiceHouseId(payment.getInvoiceId());
            houseAccessService.requireHouseAccess(invoiceHouseId);
        }

        return payment;
    }

    private Invoice loadAndAuthorizeInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> InvoiceNotFoundException.forId(invoiceId));

        Long currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.hasRole(SecurityUtils.ROLE_TENANT)
                && !securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            Tenant tenant = tenantContextResolver.linkedTenantOf(currentUserId);
            if (tenant == null || !contractTenantRepository.existsByContractIdAndTenantId(invoice.getContractId(), tenant.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to access this resource");
            }
        } else {
            Long invoiceHouseId = houseAccessService.resolveInvoiceHouseId(invoice.getId());
            houseAccessService.requireHouseAccess(invoiceHouseId);
        }

        return invoice;
    }

    private PaymentMethod loadAndValidatePaymentMethod(Long paymentMethodId, Invoice invoice) {
        PaymentMethod paymentMethod;
        Long currentUserId = securityUtils.getCurrentUserId();
        if (paymentMethodId != null) {
            paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> PaymentMethodNotFoundException.forId(paymentMethodId));

            if (!paymentMethod.getOwner().getId().equals(currentUserId)
                    && !paymentMethod.getOwner().getId().equals(invoice.getOwner().getId())) {
                throw PaymentMethodNotFoundException.forId(paymentMethodId);
            }
        } else {
            paymentMethod = paymentMethodRepository.findFirstByOwnerIdAndStatus(
                    invoice.getOwner().getId(), PaymentMethodStatus.ACTIVE)
                    .orElseGet(() -> {
                        PaymentMethod defaultMethod = PaymentMethod.builder()
                                .owner(invoice.getOwner())
                                .name("PayWay KHQR")
                                .code("KHQR")
                                .description("PayWay Sandbox KHQR Payment")
                                .status(PaymentMethodStatus.ACTIVE)
                                .build();
                        return paymentMethodRepository.save(defaultMethod);
                    });
        }

        if (paymentMethod.getStatus() != PaymentMethodStatus.ACTIVE) {
            throw new InvalidPaymentOperationException(
                    "Payment method is not active: " + paymentMethod.getName());
        }

        return paymentMethod;
    }

    private void validateInvoiceForPayment(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidPaymentOperationException("Invoice is already fully paid");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Cannot pay a cancelled invoice");
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new InvalidPaymentOperationException("Cannot pay a draft invoice");
        }
    }

    private void expireStalePayments(Long invoiceId) {
        LocalDateTime expiryCutoff = LocalDateTime.now().minusMinutes(PAYMENT_EXPIRY_MINUTES);
        for (Payment pending : paymentRepository.findByInvoiceIdAndStatusIn(invoiceId, ACTIVE_PAYMENT_STATUSES)) {
            if (pending.getCreatedAt() == null || pending.getCreatedAt().isBefore(expiryCutoff)) {
                pending.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(pending);
                log.info("Expired stale payment: paymentId={}, tranId={}, createdAt={}",
                        pending.getId(), pending.getTransactionId(), pending.getCreatedAt());
            }
        }
    }

    private void validateNoActivePayment(Long invoiceId) {
        boolean hasActive = !paymentRepository.findByInvoiceIdAndStatusIn(invoiceId, ACTIVE_PAYMENT_STATUSES).isEmpty();
        if (hasActive) {
            throw new InvalidPaymentOperationException(
                    "An active payment already exists for this invoice. Complete or wait for the existing payment to expire.");
        }
    }

    private BigDecimal calculateOutstandingAmount(Invoice invoice) {
        BigDecimal total = invoice.getTotalAmount();
        BigDecimal discount = invoice.getDiscount();
        BigDecimal outstanding = total.subtract(discount);

        if (outstanding.signum() <= 0) {
            throw new InvalidPaymentOperationException("Invoice has no outstanding amount to pay");
        }

        return outstanding;
    }

    private String generatePaymentNo() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = paymentRepository.count() + 1;
        return String.format("PAY-%s-%06d", datePart, seq);
    }

    private String generateTransactionId() {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = paymentRepository.count() + 1;
        return String.format("PAY%s%06d", datePart, seq);
    }

    private void verifyCanCreatePayment() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_TENANT)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to access this resource");
        }
    }

    private void verifyCanRecordManualPayment() {
        if (!securityUtils.hasRole(SecurityUtils.ROLE_OWNER)
                && !securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only owners and managers can record manual payments");
        }
    }
}
