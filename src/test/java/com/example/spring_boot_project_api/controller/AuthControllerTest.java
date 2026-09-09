package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.spring_boot_project_api.config.JwtAuthenticationFilter;
import com.example.spring_boot_project_api.config.SecurityConfig;
import com.example.spring_boot_project_api.dto.response.LoginResponse;
import com.example.spring_boot_project_api.dto.response.UserResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.service.AuthService;
import com.example.spring_boot_project_api.service.SecurityService;
import com.example.spring_boot_project_api.util.JwtUtil;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class})
@TestPropertySource(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "jwt.expiration-ms=900000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityService securityService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponse sampleUser() {
        return new UserResponse(1L, "johndoe", "john@example.com", "John", "Doe",
                "+1234567890", null, UserStatus.ACTIVE, null, null, null);
    }

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        when(authService.register(any())).thenReturn(sampleUser());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "firstName": "",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registerShouldReturnConflictForDuplicateUsername() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Username is already taken"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void registerShouldReturnConflictForDuplicateEmail() throws Exception {
        when(authService.register(any()))
                .thenThrow(new DuplicateResourceException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newuser",
                                  "email": "john@example.com",
                                  "password": "Password123",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void loginShouldReturnToken() throws Exception {
        when(authService.login(any(), any())).thenReturn(
                new LoginResponse("jwt-token-value", "Bearer", 900000L, sampleUser()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "johndoe",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900000))
                .andExpect(jsonPath("$.user.username").value("johndoe"));
    }

    @Test
    void loginShouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any(), any()))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "johndoe",
                                  "password": "WrongPassword1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
