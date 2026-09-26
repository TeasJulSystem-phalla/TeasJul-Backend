package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.spring_boot_project_api.dto.request.RentalContractRequest;
import com.example.spring_boot_project_api.dto.response.RentalContractResponse;
import com.example.spring_boot_project_api.enums.RentalContractStatus;
import com.example.spring_boot_project_api.enums.RoomStatus;
import com.example.spring_boot_project_api.mapper.RentalContractMapper;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.service.impl.RentalContractServiceImpl;
import com.example.spring_boot_project_api.service.RoomBookingService;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class RentalContractServiceImplTest {

    private static final String CONTRACT_NO_PATTERN = "CT-\\d{2}-\\d{2}-[0-9a-f]{9}";

    @Mock private RentalContractRepository contractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserHouseAccessRepository userHouseAccessRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private NotificationService notificationService;
    @Mock private RoomBookingService roomBookingService;
    @Spy private RentalContractMapper contractMapper = new RentalContractMapper();

    @InjectMocks private RentalContractServiceImpl rentalContractService;

    // ---- Contract creation & backend-generated identifiers ----

    @Test
    void createContract_generatesContractNumberOnBackendAndIgnoresClientValue() {
        RentalContractRequest request = request(10L, LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        request.setContractNo("CLIENT-SUPPLIED-123");
        stubCreateContract(10L, 1L);

        RentalContractResponse response = rentalContractService.createContract(request);

        assertThat(response.getId()).isEqualTo(555L);
        assertThat(response.getContractNo()).isNotEqualTo("CLIENT-SUPPLIED-123");
        assertThat(response.getContractNo()).matches(CONTRACT_NO_PATTERN);
        assertThat(response.getContractNo())
                .startsWith("CT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd")));

        ArgumentCaptor<RentalContract> captor = ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getContractNo()).matches(CONTRACT_NO_PATTERN);
    }

    @Test
    void createContract_retriesWhenGeneratedContractNumberCollides() {
        when(contractRepository.existsByContractNo(anyString())).thenReturn(true, false);
        stubCreateContract(10L, 1L);

        RentalContractResponse response = rentalContractService.createContract(
                request(10L, LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE));

        verify(contractRepository, times(2)).existsByContractNo(anyString());
        assertThat(response.getContractNo()).matches(CONTRACT_NO_PATTERN);
    }

    @Test
    void createContract_marksPastEndDateContractAsExpired() {
        stubCreateContract(10L, 1L);

        RentalContractResponse response = rentalContractService.createContract(
                request(10L, LocalDate.now().minusDays(1), RentalContractStatus.ACTIVE));

        assertThat(response.getStatus()).isEqualTo(RentalContractStatus.EXPIRED);
    }

    @Test
    void createContract_contractEndingTodayRemainsActive() {
        stubCreateContract(10L, 1L);

        RentalContractResponse response = rentalContractService.createContract(
                request(10L, LocalDate.now(), RentalContractStatus.ACTIVE));

        assertThat(response.getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
    }

    @Test
    void createContract_defaultsToDraftWhenNoStatusProvided() {
        stubCreateContract(10L, 1L);

        RentalContractResponse response = rentalContractService.createContract(
                request(10L, LocalDate.now().plusMonths(6), null));

        assertThat(response.getStatus()).isEqualTo(RentalContractStatus.DRAFT);
        assertThat(response.getOwnerId()).isEqualTo(1L);
        assertThat(response.getRoomId()).isEqualTo(10L);
    }

    // ---- Contract update keeps business rules ----

    @Test
    void updateContract_preservesBackendGeneratedContractNumber() {
        RentalContract existing = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        RentalContractRequest request = request(10L, LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        request.setContractNo("CLIENT-HACKED-42");
        stubAccessToContract(existing, LocalDate.now().plusMonths(6));

        RentalContractResponse response = rentalContractService.updateContract(1L, request);

        assertThat(response.getContractNo()).isEqualTo("CT-09-22-abc123def");
        assertThat(response.getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
    }

    @Test
    void updateContract_marksPastEndDateContractAsExpired() {
        RentalContract existing = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().minusDays(1), RentalContractStatus.ACTIVE);
        stubAccessToContract(existing, LocalDate.now().minusDays(1));

        RentalContractResponse response = rentalContractService.updateContract(1L,
                request(10L, LocalDate.now().minusDays(1), RentalContractStatus.ACTIVE));

        assertThat(response.getStatus()).isEqualTo(RentalContractStatus.EXPIRED);
    }

    // ---- Automatic expiration ----

    @Test
    void expireExpiredContracts_transitionsPastDueContractsAndFreesRoomStatus() {
        Room occupied = room(10L, 1L, 99L, "A01", RoomStatus.OCCUPIED);
        when(contractRepository.findRoomIdsOfActiveExpiredContracts(any(), any())).thenReturn(List.of(10L));
        when(contractRepository.expireActiveContractsEndedBefore(
                any(), any(), any())).thenReturn(1);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(occupied));
        when(contractRepository.existsByRoomIdAndStatus(10L, RentalContractStatus.ACTIVE)).thenReturn(false);

        int updated = rentalContractService.expireExpiredContracts();

        assertThat(updated).isEqualTo(1);
        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.AVAILABLE));
    }

    @Test
    void expireExpiredContracts_returnsZeroWhenNothingIsPastDue() {
        when(contractRepository.findRoomIdsOfActiveExpiredContracts(any(), any())).thenReturn(List.of());

        assertThat(rentalContractService.expireExpiredContracts()).isZero();
    }

    // ---- Renewal applies the inclusive end-date rule ----

    @Test
    void applyRenewal_expiredContractRenewedToFutureDateBecomesActive() {
        RentalContract expired = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().minusDays(30), RentalContractStatus.EXPIRED);
        Room available = room(10L, 1L, 99L, "A01", RoomStatus.AVAILABLE);
        LocalDate newEndDate = LocalDate.now().plusMonths(6);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(expired));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(available));
        when(contractRepository.existsByRoomIdAndStatus(10L, RentalContractStatus.ACTIVE)).thenReturn(true);

        rentalContractService.applyRenewal(1L, newEndDate);

        ArgumentCaptor<RentalContract> captor = ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getContractNo()).isEqualTo("CT-09-22-abc123def");
        assertThat(captor.getValue().getEndDate()).isEqualTo(newEndDate);
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.OCCUPIED));
    }

    @Test
    void applyRenewal_expiredContractRenewedToTodayStaysActive() {
        RentalContract expired = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().minusDays(30), RentalContractStatus.EXPIRED);
        Room available = room(10L, 1L, 99L, "A01", RoomStatus.AVAILABLE);
        LocalDate today = LocalDate.now();
        when(contractRepository.findById(1L)).thenReturn(Optional.of(expired));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(available));
        when(contractRepository.existsByRoomIdAndStatus(10L, RentalContractStatus.ACTIVE)).thenReturn(true);

        rentalContractService.applyRenewal(1L, today);

        ArgumentCaptor<RentalContract> captor = ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(today);
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
    }

    @Test
    void applyRenewal_contractRenewedToPastDateBecomesExpired() {
        RentalContract active = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().plusMonths(1), RentalContractStatus.ACTIVE);
        Room available = room(10L, 1L, 99L, "A01", RoomStatus.AVAILABLE);
        LocalDate past = LocalDate.now().minusDays(2);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(active));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(available));
        when(contractRepository.existsByRoomIdAndStatus(10L, RentalContractStatus.ACTIVE)).thenReturn(false);

        rentalContractService.applyRenewal(1L, past);

        ArgumentCaptor<RentalContract> captor = ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(past);
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalContractStatus.EXPIRED);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void applyRenewal_activeContractExtendedToFutureDateRemainsActive() {
        RentalContract aboutToExpire = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now(), RentalContractStatus.ACTIVE);
        Room occupied = room(10L, 1L, 99L, "A01", RoomStatus.OCCUPIED);
        LocalDate newEndDate = LocalDate.now().plusMonths(12);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(aboutToExpire));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(occupied));
        when(contractRepository.existsByRoomIdAndStatus(10L, RentalContractStatus.ACTIVE)).thenReturn(true);

        rentalContractService.applyRenewal(1L, newEndDate);

        ArgumentCaptor<RentalContract> captor = ArgumentCaptor.forClass(RentalContract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(newEndDate);
        assertThat(captor.getValue().getStatus()).isEqualTo(RentalContractStatus.ACTIVE);
        verify(roomRepository, never()).save(any());
    }

    // ---- Access rules keep working (owner isolation & manager access) ----

    @Test
    void getContractById_allowsOwnerAccessToOwnContract() {
        RentalContract c = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        when(contractRepository.findRoomIdsOfActiveExpiredContracts(any(), any())).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));

        RentalContractResponse response = rentalContractService.getContractById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getContractNo()).isEqualTo("CT-09-22-abc123def");
    }

    @Test
    void getContractById_deniesForeignOwnerWithoutManagerRole() {
        RentalContract c = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        when(contractRepository.findRoomIdsOfActiveExpiredContracts(any(), any())).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(999L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(false);

        assertThatThrownBy(() -> rentalContractService.getContractById(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getContractById_allowsManagerWithHouseAccess() {
        RentalContract c = contract(1L, 1L, 10L, "CT-09-22-abc123def",
                LocalDate.now().plusMonths(6), RentalContractStatus.ACTIVE);
        when(contractRepository.findRoomIdsOfActiveExpiredContracts(any(), any())).thenReturn(List.of());
        when(securityUtils.getCurrentUserId()).thenReturn(2L);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(c));
        when(securityUtils.hasRole(SecurityUtils.ROLE_MANAGER)).thenReturn(true);
        when(userHouseAccessRepository.existsByUserIdAndHouseId(2L, 10L)).thenReturn(true);

        RentalContractResponse response = rentalContractService.getContractById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    // ---- Fixtures & stubs ----

    private void stubCreateContract(long roomId, long ownerId) {
        Room room = room(roomId, ownerId, 99L, "A01", RoomStatus.AVAILABLE);
        when(securityUtils.getCurrentUserId()).thenReturn(ownerId);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(contractRepository.existsByRoomIdAndStatus(roomId, RentalContractStatus.ACTIVE)).thenReturn(false);
        when(contractRepository.save(any(RentalContract.class))).thenAnswer(inv -> {
            RentalContract c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(555L);
            }
            return c;
        });
    }

    private void stubAccessToContract(RentalContract existing, LocalDate requestedEndDate) {
        Room room = room(existing.getRoomId(), existing.getOwnerId(), 99L, "A01", RoomStatus.AVAILABLE);
        when(securityUtils.getCurrentUserId()).thenReturn(existing.getOwnerId());
        when(contractRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(roomRepository.findById(existing.getRoomId())).thenReturn(Optional.of(room));
        when(contractRepository.save(any(RentalContract.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static RentalContractRequest request(long roomId, LocalDate endDate, RentalContractStatus status) {
        RentalContractRequest request = new RentalContractRequest();
        request.setRoomId(roomId);
        request.setStartDate(LocalDate.now().minusMonths(1));
        request.setEndDate(endDate);
        request.setMonthlyRent(new BigDecimal("100.00"));
        request.setSecurityDeposit(new BigDecimal("50.00"));
        request.setDueDay(5);
        request.setStatus(status);
        return request;
    }

    private static RentalContract contract(long id, long ownerId, long roomId, String contractNo,
            LocalDate endDate, RentalContractStatus status) {
        return RentalContract.builder()
                .id(id)
                .ownerId(ownerId)
                .contractNo(contractNo)
                .roomId(roomId)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(endDate)
                .monthlyRent(new BigDecimal("100.00"))
                .securityDeposit(new BigDecimal("50.00"))
                .dueDay(5)
                .status(status)
                .build();
    }

    private static Room room(long id, long ownerId, long houseId, String number, RoomStatus status) {
        Room room = new Room();
        room.setId(id);
        room.setOwnerId(ownerId);
        room.setHouseId(houseId);
        room.setRoomNumber(number);
        room.setMonthlyRent(new BigDecimal("100.00"));
        room.setSecurityDeposit(new BigDecimal("50.00"));
        room.setCapacity(1);
        room.setStatus(status);
        return room;
    }
}
