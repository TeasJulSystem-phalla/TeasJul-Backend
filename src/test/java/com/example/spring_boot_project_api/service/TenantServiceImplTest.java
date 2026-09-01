package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.spring_boot_project_api.dto.request.CreateTenantRequest;
import com.example.spring_boot_project_api.dto.response.TenantResponse;
import com.example.spring_boot_project_api.exception.DuplicateResourceException;
import com.example.spring_boot_project_api.exception.UserNotFoundException;
import com.example.spring_boot_project_api.mapper.TenantMapper;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.TenantRepository;
import com.example.spring_boot_project_api.repository.UserHouseAccessRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.service.impl.TenantServiceImpl;
import com.example.spring_boot_project_api.util.SecurityUtils;

@ExtendWith(MockitoExtension.class)
class TenantServiceImplTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserHouseAccessRepository userHouseAccessRepository;
    @Spy
    private TenantMapper tenantMapper = new TenantMapper();
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("ownerA").email("ownerA@example.com")
                .passwordHash("h").firstName("Owner").lastName("A").status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authAs(User user) {
        UserDetails details = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(details, "n/a"));
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private User candidateUser() {
        return User.builder().id(25L).username("darasok").email("dara@gmail.com")
                .passwordHash("h").firstName("Dara").lastName("Sok").phone("012345678")
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private CreateTenantRequest request(Long userId) {
        return new CreateTenantRequest(
                userId,
                "TN-100",
                "Dara",
                "Sok",
                null,
                null,
                "012345678",
                "dara@gmail.com",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private Tenant withOwner(User owner, User linkedUser) {
        Tenant tenant = tenantMapper.toEntity(request(linkedUser != null ? linkedUser.getId() : null));
        tenant.setId(7L);
        tenant.setUser(linkedUser);
        tenant.setOwner(owner);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    @Test
    void createTenantLinksExistingUser() {
        authAs(owner);
        User candidate = candidateUser();
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.empty());
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TenantResponse response = tenantService.createTenant(request(25L));

        assertThat(response.userId()).isEqualTo(25L);
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.tenantCode()).isEqualTo("TN-100");
    }

    @Test
    void createTenantAllowsNoUserAccount() {
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response.userId()).isNull();
        assertThat(response.ownerId()).isEqualTo(1L);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createTenantRejectsForeignUserId() {
        authAs(owner);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.createTenant(request(999L)))
                .isInstanceOf(UserNotFoundException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantRejectsUserAlreadyLinked() {
        authAs(owner);
        User candidate = candidateUser();
        when(userRepository.findById(25L)).thenReturn(Optional.of(candidate));
        when(tenantRepository.findByUserId(25L)).thenReturn(Optional.of(Tenant.builder().id(1L).build()));

        assertThatThrownBy(() -> tenantService.createTenant(request(25L)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createTenantOwnerAlwaysComesFromSecurityContext() {
        // The request has no ownerId field at all, so client-supplied owner scope
        // is impossible by design; owner must resolve to the authenticated user.
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response.ownerId()).isEqualTo(1L);
        verify(tenantRepository).existsByOwnerIdAndTenantCode(1L, "TN-100");
    }

    @Test
    void noHouseOrRoomIsRequiredToCreateATenant() {
        authAs(owner);
        when(tenantRepository.existsByOwnerIdAndTenantCode(1L, "TN-100")).thenReturn(false);
        when(tenantRepository.existsByOwnerIdAndPhone(1L, "012345678")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse response = tenantService.createTenant(request(null));

        assertThat(response).isNotNull();
        verify(tenantRepository).existsByOwnerIdAndTenantCode(1L, "TN-100");
    }

    @Test
    void tenantMapperHandlesNullUserWhenMappingResponse() {
        Tenant tenant = withOwner(owner, null);
        tenant.setId(7L);

        TenantResponse response = tenantMapper.toResponse(tenant);

        assertThat(response.userId()).isNull();
        assertThat(response.id()).isEqualTo(7L);
    }
}