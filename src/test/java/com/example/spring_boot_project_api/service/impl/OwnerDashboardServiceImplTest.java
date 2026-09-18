package com.example.spring_boot_project_api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.spring_boot_project_api.dto.response.OwnerDashboardResponse;
import com.example.spring_boot_project_api.enums.InvoiceStatus;
import com.example.spring_boot_project_api.enums.PaymentStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.MaintenanceRequest;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.MaintenanceRequestRepository;
import com.example.spring_boot_project_api.repository.PaymentRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.HouseAccessService;

@ExtendWith(MockitoExtension.class)
class OwnerDashboardServiceImplTest {

    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private RentalContractRepository contractRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private MaintenanceRequestRepository maintenanceRequestRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private HouseAccessService houseAccessService;

    private OwnerDashboardServiceImpl service;
    private RentalContract contract;
    private Invoice invoice;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new OwnerDashboardServiceImpl(
                roomRepository, houseRepository, contractRepository, invoiceRepository,
                paymentRepository, maintenanceRequestRepository, tenantRepository, houseAccessService);

        contract = RentalContract.builder().id(20L).roomId(10L).build();
        invoice = Invoice.builder().id(30L).contractId(20L).status(InvoiceStatus.PAID).build();
        room = new Room();
        room.setId(10L);
        room.setHouseId(1L);
        room.setRoomNumber(null);
        room.setStatus(RoomStatus.AVAILABLE);

        when(houseAccessService.getAccessibleHouseIds()).thenReturn(List.of(1L));
        when(roomRepository.countByHouseIdIn(List.of(1L))).thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(List.of(1L), RoomStatus.OCCUPIED)).thenReturn(0L);
        when(roomRepository.countByHouseIdInAndStatus(List.of(1L), RoomStatus.AVAILABLE)).thenReturn(1L);
        when(roomRepository.countByHouseIdInAndStatus(List.of(1L), RoomStatus.MAINTENANCE)).thenReturn(0L);
        when(tenantRepository.countActiveTenantsByHouseIds(eq(List.of(1L)), any())).thenReturn(0L);
        when(contractRepository.findByRoomHouseIdIn(List.of(1L))).thenReturn(List.of(contract));
        when(invoiceRepository.findByContractIdIn(List.of(20L))).thenReturn(List.of(invoice));
        when(roomRepository.findByHouseIdIn(List.of(1L))).thenReturn(List.of(room));
        when(houseRepository.findAllById(List.of(1L))).thenReturn(List.of(house(1L, "House One")));
        when(maintenanceRequestRepository.findAllByHouseIdInOrderByCreatedAtDesc(List.of(1L)))
                .thenReturn(List.of());
    }

    @Test
    void nullRoomNumberAndNullAmountDoNotBreakRecentPayments() {
        Payment payment = payment(40L, PaymentStatus.PAID, null);
        when(paymentRepository.findByInvoiceIdIn(List.of(30L))).thenReturn(List.of(payment));

        OwnerDashboardResponse dashboard = service.getDashboard(null, null, null);

        assertThat(dashboard.recentPayments()).singleElement().satisfies(recent -> {
            assertThat(recent.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(recent.roomNumber()).isEqualTo("UNKNOWN");
        });
    }

    @Test
    void nullPaymentStatusDoesNotBreakDashboard() {
        Payment payment = payment(40L, null, BigDecimal.TEN);
        when(paymentRepository.findByInvoiceIdIn(List.of(30L))).thenReturn(List.of(payment));

        OwnerDashboardResponse dashboard = service.getDashboard(null, null, null);

        assertThat(dashboard.recentPayments()).isEmpty();
    }

    @Test
    void duplicateRoomKeysAreMergedAndValidPaymentIsPreserved() {
        Room duplicate = new Room();
        duplicate.setId(10L);
        duplicate.setHouseId(1L);
        duplicate.setRoomNumber("A-1");
        when(roomRepository.findByHouseIdIn(List.of(1L))).thenReturn(List.of(room, duplicate));
        when(paymentRepository.findByInvoiceIdIn(List.of(30L)))
                .thenReturn(List.of(payment(40L, PaymentStatus.PAID, BigDecimal.valueOf(25))));

        OwnerDashboardResponse dashboard = service.getDashboard(null, null, null);

        assertThat(dashboard.recentPayments()).singleElement().satisfies(recent -> {
            assertThat(recent.amount()).isEqualByComparingTo("25");
            assertThat(recent.status()).isEqualTo("PAID");
        });
    }

    @Test
    void emptyPaymentListProducesNoRecentPayments() {
        when(paymentRepository.findByInvoiceIdIn(List.of(30L))).thenReturn(List.of());

        OwnerDashboardResponse dashboard = service.getDashboard(null, null, null);

        assertThat(dashboard.recentPayments()).isEmpty();
    }

    private Payment payment(Long id, PaymentStatus status, BigDecimal amount) {
        return Payment.builder().id(id).invoiceId(30L).status(status).amount(amount).build();
    }

    private House house(Long id, String name) {
        House house = new House();
        house.setId(id);
        house.setName(name);
        return house;
    }
}
