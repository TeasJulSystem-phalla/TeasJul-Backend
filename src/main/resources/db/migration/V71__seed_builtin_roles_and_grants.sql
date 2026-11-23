-- =====================================================================
-- V71: Ensure the built-in system roles exist and (re)apply their grants.
--
-- Why this migration exists
-- -------------------------
-- The application hard-requires the OWNER and TENANT roles: account/owner
-- creation and tenant-invitation acceptance call
-- RoleRepository.findByCode(...).orElseThrow(...), and the whole authorization
-- model is built around OWNER / MANAGER / TENANT / SUPER_ADMIN
-- (SecurityUtils.ROLE_*). Only SUPER_ADMIN was ever seeded by Flyway (V49);
-- the other three were historically created out-of-band, so a database
-- rebuilt purely from the migration chain -- or any fresh install -- is
-- missing them. Worse, V46's role_permissions grants run BEFORE those roles
-- exist, so they silently match zero roles and insert nothing.
--
-- This migration is intentionally NON-DESTRUCTIVE and IDEMPOTENT:
--   * roles are upserted on the unique roles.code (same idiom as V49)
--   * role_permissions use INSERT IGNORE, mirroring V46/V54 exactly, so
--     existing grants (including SUPER_ADMIN's, which V49/V54/V56 created)
--     are never removed or duplicated
--   * no user account is created -- this only restores role/permission data
--   * role ids are left to AUTO_INCREMENT: role ids are never referenced by
--     number anywhere in the code (everything matches on role code), and the
--     existing SUPER_ADMIN id and its user_roles rows are preserved as-is.
--
-- Role names/descriptions reproduce the pre-reset values; the permission
-- grants reproduce the current authorization design (V46 + V54).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Built-in roles (idempotent upsert on the unique role code).
--    Names/descriptions match the pre-reset database.
-- ---------------------------------------------------------------------
INSERT INTO roles (name, code, description, is_system_role, created_at, updated_at)
VALUES
    ('Owner',         'OWNER',   'Owns houses',             1, NOW(), NOW()),
    ('House Manager', 'MANAGER', 'Manages assigned houses', 1, NOW(), NOW()),
    ('Tenant',        'TENANT',  'Tennet rent the house',   1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name           = VALUES(name),
    description    = VALUES(description),
    is_system_role = VALUES(is_system_role);

-- ---------------------------------------------------------------------
-- 2) OWNER -> every permission (mirrors V46).
--    OWNER also short-circuits to full access in SecurityUtils; these rows
--    keep the permission catalog consistent with the documented design.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'OWNER';

-- ---------------------------------------------------------------------
-- 3) MANAGER -> all business modules + HOUSE_ACCESS_VIEW (mirrors V46).
--    Deliberately NOT the USER or SETTING modules.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'MANAGER'
  AND (p.module IN ('HOUSE', 'ROOM', 'TENANT', 'CONTRACT', 'PAYMENT', 'INVOICE',
                    'MAINTENANCE', 'EXPENSE', 'AMENITY', 'UTILITY', 'REPORT',
                    'DASHBOARD', 'NOTIFICATION', 'ASSISTANT')
       OR (p.module = 'HOUSE_ACCESS' AND p.code = 'HOUSE_ACCESS_VIEW'));

-- ---------------------------------------------------------------------
-- 4) TENANT -> self-service permissions only (mirrors V46).
-- ---------------------------------------------------------------------
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'TENANT'
  AND p.code IN ('TENANT_VIEW', 'PAYMENT_VIEW', 'PAYMENT_CREATE', 'INVOICE_VIEW',
                 'MAINTENANCE_VIEW', 'MAINTENANCE_CREATE', 'DASHBOARD_VIEW',
                 'NOTIFICATION_VIEW', 'ASSISTANT_VIEW');

-- ---------------------------------------------------------------------
-- 5) OWNER / MANAGER -> grouped-listing permissions (mirrors V54).
--    SUPER_ADMIN already holds these from V54; INSERT IGNORE keeps it idempotent.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO role_permissions (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('OWNER', 'MANAGER')
  AND p.code IN ('LISTING_VIEW', 'LISTING_MANAGE');
