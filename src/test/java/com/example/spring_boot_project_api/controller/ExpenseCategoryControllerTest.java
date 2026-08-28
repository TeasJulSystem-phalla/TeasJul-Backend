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
import com.example.spring_boot_project_api.dto.request.CreateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.request.UpdateExpenseCategoryRequest;
import com.example.spring_boot_project_api.dto.response.ExpenseCategoryResponse;
import com.example.spring_boot_project_api.model.ExpenseCategoryStatus;
import com.example.spring_boot_project_api.service.ExpenseCategoryService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(ExpenseCategoryController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class })
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class ExpenseCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExpenseCategoryService expenseCategoryService;

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
    void createCategoryShouldReturnCreated() throws Exception {
        ExpenseCategoryResponse response = sampleResponse();

        when(expenseCategoryService.createCategory(any(CreateExpenseCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/expense-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "name": "Electricity",
                                    "code": "ELEC",
                                    "description": "Electric bills",
                                    "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electricity"))
                .andExpect(jsonPath("$.code").value("ELEC"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCategoryShouldReturnBadRequestWhenInvalid() throws Exception {
        mockMvc.perform(post("/api/expense-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "name": "",
                                    "code": "",
                                    "status": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getMyCategoriesShouldReturnOk() throws Exception {
        when(expenseCategoryService.getMyCategories())
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/expense-categories")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electricity"));
    }

    @Test
    void getCategoryByIdShouldReturnOk() throws Exception {
        when(expenseCategoryService.getCategoryById(1L))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/expense-categories/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electricity"));
    }

    @Test
    void updateCategoryShouldReturnOk() throws Exception {
        ExpenseCategoryResponse updated = new ExpenseCategoryResponse(
                1L, 1L, "Water", "WATER", "Water bills",
                ExpenseCategoryStatus.INACTIVE, LocalDateTime.now(), LocalDateTime.now());

        when(expenseCategoryService.updateCategory(anyLong(), any(UpdateExpenseCategoryRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/expense-categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken())
                        .content("""
                                {
                                    "name": "Water",
                                    "code": "WATER",
                                    "description": "Water bills",
                                    "status": "INACTIVE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Water"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deleteCategoryShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/expense-categories/1")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void protectedEndpointShouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/expense-categories"))
                .andExpect(status().isUnauthorized());
    }

    private ExpenseCategoryResponse sampleResponse() {
        return new ExpenseCategoryResponse(
                1L, 1L, "Electricity", "ELEC", "Electric bills",
                ExpenseCategoryStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    private String validToken() {
        return jwtUtil.generateToken("johndoe");
    }
}
