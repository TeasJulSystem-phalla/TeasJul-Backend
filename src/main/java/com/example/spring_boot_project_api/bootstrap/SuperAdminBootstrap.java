package com.example.spring_boot_project_api.bootstrap;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.model.UserStatus;
import com.example.spring_boot_project_api.repository.RoleRepository;
import com.example.spring_boot_project_api.repository.UserRepository;
import com.example.spring_boot_project_api.repository.UserRoleRepository;
import com.example.spring_boot_project_api.util.EmailValidator;
import com.example.spring_boot_project_api.util.SecurityUtils;

/**
 * Idempotent provisioning of the very first {@code SUPER_ADMIN} account.
 *
 * <p>A fresh database has the {@code SUPER_ADMIN} role row seeded by Flyway, but
 * no user, and there is no self-service way to obtain the role (role assignment
 * itself requires an OWNER or SUPER_ADMIN caller). This runner closes that
 * bootstrap gap by creating the account once, from environment-provided
 * credentials.</p>
 *
 * <p>Security and safety properties:</p>
 * <ul>
 *   <li>The password is read from the environment, hashed with the same BCrypt
 *       {@link PasswordEncoder} used by the login flow, and never logged.</li>
 *   <li>Blank/unset credentials make the whole bootstrap a no-op, so a normal
 *       (production) start without these variables cannot create an account.</li>
 *   <li>Idempotent: if a user with the configured username or email already
 *       exists it is never recreated, and the SUPER_ADMIN role is only added
 *       when missing. Repeated starts therefore create no duplicates.</li>
 * </ul>
 */
@Component
public class SuperAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.super-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.bootstrap.super-admin.username:}")
    private String username;

    @Value("${app.bootstrap.super-admin.email:}")
    private String email;

    @Value("${app.bootstrap.super-admin.password:}")
    private String password;

    @Value("${app.bootstrap.super-admin.first-name:Super}")
    private String firstName;

    @Value("${app.bootstrap.super-admin.last-name:Admin}")
    private String lastName;

    @Value("${app.bootstrap.super-admin.phone:}")
    private String phone;

    public SuperAdminBootstrap(UserRepository userRepository, RoleRepository roleRepository,
            UserRoleRepository userRoleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("SUPER_ADMIN bootstrap is disabled; skipping");
            return;
        }
        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            // Not configured -> do nothing. This keeps a normal start side-effect free.
            log.info("SUPER_ADMIN bootstrap skipped: username/email/password not fully configured "
                    + "(set SUPER_ADMIN_USERNAME, SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD to provision)");
            return;
        }

        Role superAdminRole = ensureSuperAdminRole();

        Optional<User> existing = userRepository.findByUsername(username.trim())
                .or(() -> userRepository.findByEmail(email.trim()));
        if (existing.isPresent()) {
            User user = existing.get();
            ensureRoleAssignment(user, superAdminRole);
            log.info("SUPER_ADMIN bootstrap: account already present (id={}, username={}); SUPER_ADMIN role ensured",
                    user.getId(), user.getUsername());
            return;
        }

        if (!EmailValidator.isOnInternalDomain(email)) {
            // Internal accounts must log in on the corporate domain. Fail soft so a
            // misconfigured email can never take the API down at startup.
            log.error("SUPER_ADMIN bootstrap: email '{}' is not on the internal domain @{}; "
                    + "account was NOT created", email.trim(), EmailValidator.INTERNAL_DOMAIN);
            return;
        }

        User user = User.builder()
                .username(username.trim())
                .email(email.trim())
                .passwordHash(passwordEncoder.encode(password))
                .firstName(isBlank(firstName) ? "Super" : firstName.trim())
                .lastName(isBlank(lastName) ? "Admin" : lastName.trim())
                .phone(isBlank(phone) ? null : phone.trim())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        ensureRoleAssignment(saved, superAdminRole);
        log.info("SUPER_ADMIN bootstrap: created account id={} username={}",
                saved.getId(), saved.getUsername());
    }

    private Role ensureSuperAdminRole() {
        return roleRepository.findByCode(SecurityUtils.ROLE_SUPER_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("Super Admin")
                        .code(SecurityUtils.ROLE_SUPER_ADMIN)
                        .description("System super administrator with full access")
                        .isSystemRole(true)
                        .build()));
    }

    private void ensureRoleAssignment(User user, Role role) {
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            return;
        }
        userRoleRepository.save(UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .build());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
