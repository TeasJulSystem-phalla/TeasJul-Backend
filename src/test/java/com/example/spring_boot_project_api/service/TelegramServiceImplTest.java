package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.example.spring_boot_project_api.config.TelegramBotConfig;
import com.example.spring_boot_project_api.model.ContractTenant;
import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.Invoice;
import com.example.spring_boot_project_api.model.Payment;
import com.example.spring_boot_project_api.model.PaymentMethod;
import com.example.spring_boot_project_api.model.RentalContract;
import com.example.spring_boot_project_api.model.Room;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.repository.ContractTenantRepository;
import com.example.spring_boot_project_api.repository.HouseRepository;
import com.example.spring_boot_project_api.repository.InvoiceRepository;
import com.example.spring_boot_project_api.repository.RentalContractRepository;
import com.example.spring_boot_project_api.repository.RoomRepository;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.service.impl.TelegramServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramServiceImplTest {

    @Mock private RestTemplate restTemplate;
    @Mock private TelegramBotConfig telegramBotConfig;
    @Mock private OwnerSettingService ownerSettingService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RentalContractRepository rentalContractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private HouseRepository houseRepository;
    @Mock private ContractTenantRepository contractTenantRepository;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks private TelegramServiceImpl telegramService;

    private User owner;
    private Payment payment;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner1").email("owner@test.com").build();

        House house = House.builder().id(5L).name("lanh phallla").build();
        Room room = new Room();
        room.setId(7L);
        room.setHouseId(5L);
        room.setRoomNumber("A1");
        RentalContract contract = RentalContract.builder()
                .id(100L)
                .roomId(7L)
                .contractNo("CT-2026-001")
                .build();
        Invoice invoice = Invoice.builder()
                .id(10L)
                .owner(owner)
                .invoiceNo("INV-2026-0001")
                .contractId(100L)
                .totalAmount(new BigDecimal("250.00"))
                .build();
        Tenant tenant = Tenant.builder()
                .id(20L)
                .firstName("Sok")
                .lastName("Dara")
                .build();
        ContractTenant link = ContractTenant.builder()
                .id(1L)
                .contractId(100L)
                .tenantId(20L)
                .isPrimary(true)
                .joinedAt(LocalDate.of(2026, 1, 1))
                .build();
        PaymentMethod method = PaymentMethod.builder().id(3L).name("ABA KHQR").build();

        payment = Payment.builder()
                .id(300L)
                .owner(owner)
                .invoiceId(10L)
                .paymentNo("PAY-20260909-000001")
                .transactionId("PAY20260909000001")
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .exchangeRate(new BigDecimal("4100"))
                .exchangeRateTo("KHR")
                .paymentMethod(method)
                .paidAt(LocalDateTime.of(2026, 9, 9, 14, 30))
                .build();

        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(rentalContractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(roomRepository.findById(7L)).thenReturn(Optional.of(room));
        when(houseRepository.findById(5L)).thenReturn(Optional.of(house));
        when(contractTenantRepository.findByContractId(100L)).thenReturn(List.of(link));
        when(tenantRepository.findById(20L)).thenReturn(Optional.of(tenant));
    }

    @Test
    void sendPaymentSuccess_skipsWhenNoCredentialsConfigured() {
        when(ownerSettingService.getTelegramBotToken(any())).thenReturn("");
        when(ownerSettingService.getTelegramChatId(any())).thenReturn("");
        when(telegramBotConfig.getBotToken()).thenReturn("");
        when(telegramBotConfig.getChatId()).thenReturn("");

        telegramService.sendPaymentSuccess(payment);

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void sendPaymentSuccess_fromSettingsCredentials() {
        when(ownerSettingService.getTelegramBotToken(any())).thenReturn("SETTINGS_TOKEN");
        when(ownerSettingService.getTelegramChatId(any())).thenReturn("123456");

        telegramService.sendPaymentSuccess(payment);

        ArgumentCaptor<HttpEntity<Map<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.telegram.org/botSETTINGS_TOKEN/sendMessage"),
                entityCaptor.capture(),
                eq(String.class));

        Map<String, String> body = entityCaptor.getValue().getBody();
        assertThat(body).containsEntry("chat_id", "123456");
        String text = (String) body.get("text");
        assertThat(text)
                .contains("Payment successful")
                .contains("Invoice: INV-2026-0001")
                .contains("Tenant: Sok Dara")
                .contains("House: lanh phallla (Room A1)")
                .contains("Amount: $250.00 (1025000 KHR)")
                .contains("Method: ABA KHQR")
                .contains("Ref: PAY-20260909-000001")
                .contains("Time: 09/09/2026 14:30");
    }

    @Test
    void sendPaymentSuccess_fallsBackToEnvironmentConfig() {
        when(ownerSettingService.getTelegramBotToken(any())).thenReturn("");
        when(ownerSettingService.getTelegramChatId(any())).thenReturn("");
        when(telegramBotConfig.getBotToken()).thenReturn("ENV_TOKEN");
        when(telegramBotConfig.getChatId()).thenReturn("-999");

        telegramService.sendPaymentSuccess(payment);

        verify(restTemplate).postForEntity(
                eq("https://api.telegram.org/botENV_TOKEN/sendMessage"),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    void sendPaymentCreated_sendsInitiatedMessage() {
        when(ownerSettingService.getTelegramBotToken(any())).thenReturn("TOKEN");
        when(ownerSettingService.getTelegramChatId(any())).thenReturn("123456");

        telegramService.sendPaymentCreated(payment);

        ArgumentCaptor<HttpEntity<Map<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.telegram.org/botTOKEN/sendMessage"),
                entityCaptor.capture(),
                eq(String.class));

        String text = entityCaptor.getValue().getBody().get("text");
        assertThat(text)
                .contains("New payment initiated")
                .contains("Invoice: INV-2026-0001")
                .contains("Ref: PAY-20260909-000001")
                .doesNotContain("Payment successful");
    }

    @Test
    void sendPaymentSuccess_doesNotFailOnMissingInvoiceDetails() {
        when(ownerSettingService.getTelegramBotToken(any())).thenReturn("TOKEN");
        when(ownerSettingService.getTelegramChatId(any())).thenReturn("123456");
        when(invoiceRepository.findById(10L)).thenReturn(Optional.empty());

        telegramService.sendPaymentSuccess(payment);

        ArgumentCaptor<HttpEntity<Map<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://api.telegram.org/botTOKEN/sendMessage"),
                entityCaptor.capture(),
                eq(String.class));

        String text = entityCaptor.getValue().getBody().get("text");
        assertThat(text)
                .contains("Payment successful")
                .contains("Amount: $250.00 (1025000 KHR)")
                .contains("Method: ABA KHQR")
                .contains("Ref: PAY-20260909-000001");
    }
}