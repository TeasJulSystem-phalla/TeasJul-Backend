package com.example.spring_boot_project_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.spring_boot_project_api.model.House;
import com.example.spring_boot_project_api.model.HouseStatus;
import com.example.spring_boot_project_api.model.Role;
import com.example.spring_boot_project_api.model.Tenant;
import com.example.spring_boot_project_api.model.TenantStatus;
import com.example.spring_boot_project_api.model.User;
import com.example.spring_boot_project_api.model.UserHouseAccess;
import com.example.spring_boot_project_api.model.UserHouseAccessId;
import com.example.spring_boot_project_api.model.UserRole;
import com.example.spring_boot_project_api.model.UserRoleId;
import com.example.spring_boot_project_api.model.UserStatus;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class UserScopeRepositoryTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4.11")
            .withDatabaseName("teasjul_test_" + UUID.randomUUID().toString().replace("-", ""))
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.clean-disabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private org.springframework.boot.jpa.test.autoconfigure.TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User persistUser(String username, String email, UserStatus status) {
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash("$2a$10$somehashedvaluehere")
                .firstName("F")
                .lastName("L")
                .status(status)
                .build();
        return entityManager.persistFlushFind(user);
    }

    private Role resolveRole(String code) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role role = Role.builder()
                    .code(code)
                    .name(code)
                    .description(code)
                    .isSystemRole(true)
                    .build();
            return entityManager.persistFlushFind(role);
        });
    }

    private House persistHouse(User owner) {
        House house = House.builder()
                .owner(owner)
                .name("H")
                .slug("h-" + UUID.randomUUID().toString().substring(0, 8))
                .code("H" + UUID.randomUUID().toString().substring(0, 6))
                .address("Addr")
                .status(HouseStatus.ACTIVE)
                .build();
        return entityManager.persistFlushFind(house);
    }

    private void assignRole(User user, Role role) {
        entityManager.persist(UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .build());
        entityManager.flush();
    }

    private void assignManagerToHouse(User manager, House house) {
        entityManager.persist(UserHouseAccess.builder()
                .id(new UserHouseAccessId(manager.getId(), house.getId()))
                .user(manager)
                .house(house)
                .build());
        entityManager.flush();
    }

    @Test
    void ownerScope_excludesOutOfScopeUsers() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        Role ownerRole = resolveRole("OWNER");
        Role superAdminRole = resolveRole("SUPER_ADMIN");
        User owner = persistUser("owner" + s, "owner" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(owner, ownerRole);
        User outOfScope = persistUser("oos" + s, "oos" + s + "@x.com", UserStatus.ACTIVE);
        User unrelated = persistUser("unrel" + s, "unrel" + s + "@x.com", UserStatus.ACTIVE);
        User superAdmin = persistUser("sa" + s, "sa" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(superAdmin, superAdminRole);
        User otherOwner = persistUser("othero" + s, "othero" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(otherOwner, ownerRole);
        House house = persistHouse(owner);
        User inScopeViaTenant = persistUser("tsc" + s, "tsc" + s + "@x.com", UserStatus.ACTIVE);
        Tenant linked = Tenant.builder()
                .user(inScopeViaTenant)
                .owner(owner)
                .house(house)
                .tenantCode("T2" + s)
                .firstName("F")
                .lastName("L")
                .phone("1" + s)
                .status(TenantStatus.ACTIVE)
                .build();
        entityManager.persistFlushFind(linked);
        entityManager.flush();
        Collection<Long> ownerIds = List.of(owner.getId());
        Collection<Long> houseIds = List.of(house.getId());
        Page<User> page = userRepository.findAvailableForTenantInOwnerScope(null, owner.getId(), ownerIds, houseIds, PageRequest.of(0, 20));
        List<Long> ids = page.getContent().stream().map(User::getId).toList();

        // The caller is always inside its own scope and, having no tenant profile,
        // is the only candidate available for linking.
        assertThat(ids).containsExactly(owner.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);

        // A user already attached to a tenant profile (even inside the owner's
        // scope) cannot be linked to a second profile, so it is not available.
        assertThat(ids).doesNotContain(inScopeViaTenant.getId());
        assertThat(ids).doesNotContain(linked.getUser().getId());

        // Users outside the scope, other owners and super admins stay hidden.
        assertThat(ids).doesNotContain(outOfScope.getId());
        assertThat(ids).doesNotContain(unrelated.getId());
        assertThat(ids).doesNotContain(superAdmin.getId());
        assertThat(ids).doesNotContain(otherOwner.getId());
    }

    @Test
    void ownerScope_selfIdRevealsTheCallerWithoutWideningTheScope() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        Role ownerRole = resolveRole("OWNER");
        User owner = persistUser("owner" + s, "owner" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(owner, ownerRole);
        User caller = persistUser("caller" + s, "caller" + s + "@x.com", UserStatus.ACTIVE);
        House house = persistHouse(owner);
        entityManager.flush();
        Collection<Long> ownerIds = List.of(owner.getId());
        Collection<Long> houseIds = List.of(house.getId());

        // A plain user holding no role and no tenant link is not visible to the owner.
        List<Long> seenByOwner = userRepository
                .findAvailableForTenantInOwnerScope(null, owner.getId(), ownerIds, houseIds, PageRequest.of(0, 20))
                .getContent().stream().map(User::getId).toList();
        assertThat(seenByOwner).containsExactly(owner.getId());
        assertThat(seenByOwner).doesNotContain(caller.getId());

        // The very same user, acting as the caller, can always see itself.
        List<Long> seenBySelf = userRepository
                .findAvailableForTenantInOwnerScope(null, caller.getId(), ownerIds, houseIds, PageRequest.of(0, 20))
                .getContent().stream().map(User::getId).toList();
        assertThat(seenBySelf).containsExactly(caller.getId());
    }

    @Test
    void existsInOwnerScope_behaviors() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        Role ownerRole = resolveRole("OWNER");
        User owner = persistUser("owner" + s, "owner" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(owner, ownerRole);
        User manager = persistUser("mgr" + s, "mgr" + s + "@x.com", UserStatus.ACTIVE);
        User unrelated = persistUser("unrel" + s, "unrel" + s + "@x.com", UserStatus.ACTIVE);
        House house = persistHouse(owner);
        assignManagerToHouse(manager, house);
        Tenant linked = Tenant.builder()
                .user(null)
                .owner(owner)
                .house(house)
                .tenantCode("T" + s)
                .firstName("F")
                .lastName("L")
                .phone("0" + s)
                .status(TenantStatus.ACTIVE)
                .build();
        linked = entityManager.persistFlushFind(linked);
        User linkedUser = persistUser("lu" + s, "lu" + s + "@x.com", UserStatus.ACTIVE);
        linked.setUser(linkedUser);
        entityManager.persistFlushFind(linked);
        entityManager.flush();
        Collection<Long> ownerIdsOwner = List.of(owner.getId());
        Collection<Long> houseIdsHouse = List.of(house.getId());
        assertThat(userRepository.existsInOwnerScope(owner.getId(), owner.getId(), ownerIdsOwner, houseIdsHouse)).isTrue();
        assertThat(userRepository.existsInOwnerScope(linkedUser.getId(), owner.getId(), ownerIdsOwner, houseIdsHouse)).isTrue();
        assertThat(userRepository.existsInOwnerScope(unrelated.getId(), owner.getId(), ownerIdsOwner, houseIdsHouse)).isFalse();
        Collection<Long> ownerIdsMgr = List.of(owner.getId());
        assertThat(userRepository.existsInOwnerScope(linkedUser.getId(), manager.getId(), ownerIdsMgr, houseIdsHouse)).isTrue();
        assertThat(userRepository.existsInOwnerScope(unrelated.getId(), manager.getId(), ownerIdsMgr, houseIdsHouse)).isFalse();
    }

    @Test
    void ownerScope_noEligibleCandidates_returnsEmptyAndDoesNotReturnGlobalPool() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        Role ownerRole = resolveRole("OWNER");
        Role superAdminRole = resolveRole("SUPER_ADMIN");
        User owner = persistUser("owner" + s, "owner" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(owner, ownerRole);
        // The caller already owns a tenant profile, so it is not an available
        // candidate either: the scope legitimately contains no eligible user.
        Tenant own = Tenant.builder()
                .user(owner)
                .owner(owner)
                .tenantCode("TO" + s)
                .firstName("F")
                .lastName("L")
                .phone("9" + s)
                .status(TenantStatus.ACTIVE)
                .build();
        entityManager.persistFlushFind(own);
        User unrelated1 = persistUser("u1" + s, "u1" + s + "@x.com", UserStatus.ACTIVE);
        User unrelated2 = persistUser("u2" + s, "u2" + s + "@x.com", UserStatus.ACTIVE);
        User superAdmin = persistUser("sa" + s, "sa" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(superAdmin, superAdminRole);
        User otherOwner = persistUser("oo" + s, "oo" + s + "@x.com", UserStatus.ACTIVE);
        assignRole(otherOwner, ownerRole);
        House otherHouse = persistHouse(otherOwner);
        User inOtherScope = persistUser("ios" + s, "ios" + s + "@x.com", UserStatus.ACTIVE);
        Tenant linkedOther = Tenant.builder()
                .user(inOtherScope)
                .owner(otherOwner)
                .house(otherHouse)
                .tenantCode("T" + s)
                .firstName("F")
                .lastName("L")
                .phone("0" + s)
                .status(TenantStatus.ACTIVE)
                .build();
        entityManager.persistFlushFind(linkedOther);
        entityManager.flush();
        Page<User> scoped = userRepository.findAvailableForTenantInOwnerScope(null, owner.getId(),
                List.of(owner.getId()), List.of(), PageRequest.of(0, 20));
        assertThat(scoped.getContent()).isEmpty();
        assertThat(scoped.getTotalElements()).isZero();
        assertThat(scoped.getContent()).extracting(User::getId)
                .doesNotContain(unrelated1.getId(), unrelated2.getId(), superAdmin.getId(),
                        otherOwner.getId(), inOtherScope.getId(), owner.getId());
    }

    @Test
    void ownerScope_searchFiltersByNameUsernameEmailPhone() {
        String s = UUID.randomUUID().toString().substring(0, 6);
        Role ownerRole = resolveRole("OWNER");
        User owner = persistUser("searchuser" + s, "search" + s + "@example.com", UserStatus.ACTIVE);
        owner.setFirstName("Dara");
        owner.setLastName("Chan");
        owner.setPhone("099" + s);
        entityManager.persistAndFlush(owner);
        assignRole(owner, ownerRole);
        // An out-of-scope user that would match every search term is never returned.
        User outOfScope = persistUser("nomatch" + s, "nomatch" + s + "@example.com", UserStatus.ACTIVE);
        outOfScope.setFirstName("Dara");
        outOfScope.setLastName("Chan");
        outOfScope.setPhone("099" + s);
        entityManager.persistAndFlush(outOfScope);
        entityManager.flush();

        Collection<Long> ownerIds = List.of(owner.getId());
        Collection<Long> houseIds = List.of();

        assertThat(candidates("Dara", owner.getId(), ownerIds, houseIds)).containsExactly(owner.getId());
        assertThat(candidates("Chan", owner.getId(), ownerIds, houseIds)).containsExactly(owner.getId());
        assertThat(candidates("searchuser" + s, owner.getId(), ownerIds, houseIds)).containsExactly(owner.getId());
        assertThat(candidates("search" + s + "@example.com", owner.getId(), ownerIds, houseIds)).containsExactly(owner.getId());
        assertThat(candidates("099" + s, owner.getId(), ownerIds, houseIds)).containsExactly(owner.getId());
        assertThat(candidates("notfoundxyz", owner.getId(), ownerIds, houseIds)).isEmpty();
    }

    private List<Long> candidates(String q, Long selfId, Collection<Long> ownerIds, Collection<Long> houseIds) {
        return userRepository.findAvailableForTenantInOwnerScope(q, selfId, ownerIds, houseIds,
                PageRequest.of(0, 20)).getContent().stream().map(User::getId).toList();
    }
}
