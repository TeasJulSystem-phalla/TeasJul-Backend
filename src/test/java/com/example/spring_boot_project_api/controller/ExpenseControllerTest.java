package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.request.CreateExpenseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseResponse;
import com.example.spring_boot_project_api.model.ExpenseStatus;
import com.example.spring_boot_project_api.service.ExpenseService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(ExpenseController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class })
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = User.withUsername("johndoe")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername("johndoe")).thenReturn(userDetails);
    }

    @Test
    void createExpenseShouldReturnCreated() throws Exception {
        ExpenseResponse response = sampleResponse();

        when(expenseService.createExpense(any(CreateExpenseRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "expenseNo": "EXP-001",
                                    "categoryId": 1,
                                    "description": "Electric bill",
                                    "amount": 150.00,
                                    "expenseDate": "2026-01-15",
                                    "status": "PAID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseNo").value("EXP-001"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void createExpenseShouldReturnBadRequestWhenInvalid() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "expenseNo": "",
                                    "categoryId": null,
                                    "description": "",
                                    "amount": -1,
                                    "expenseDate": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getMyExpensesShouldReturnOk() throws Exception {
        when(expenseService.getMyExpenses())
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/expenses")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expenseNo").value("EXP-001"));
    }

    @Test
    void getExpenseByIdShouldReturnOk() throws Exception {
        when(expenseService.getExpenseById(1L))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/expenses/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseNo").value("EXP-001"));
    }

    @Test
    void updateExpenseShouldReturnOk() throws Exception {
        ExpenseResponse updated = new ExpenseResponse(
                1L, 1L, "EXP-001", null, 1L, "Updated bill",
                BigDecimal.valueOf(200.00), LocalDate.now(), null,
                ExpenseStatus.PENDING, 1L, LocalDateTime.now(), LocalDateTime.now());

        when(expenseService.updateExpense(anyLong(), any(UpdateExpenseRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "description": "Updated bill",
                                    "amount": 200.00,
                                    "status": "PENDING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated bill"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deleteExpenseShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/expenses/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    private ExpenseResponse sampleResponse() {
        return new ExpenseResponse(
                1L, 1L, "EXP-001", null, 1L, "Electric bill",
                BigDecimal.valueOf(150.00), LocalDate.now(), null,
                ExpenseStatus.PAID, 1L, LocalDateTime.now(), LocalDateTime.now());
    }

    private String validToken() {
        return jwtUtil.generateToken("johndoe");
    }
}
