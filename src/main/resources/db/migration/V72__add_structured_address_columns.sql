-- V72: Additive structured-address columns for tenants, houses and rental contracts.
--
-- These nullable columns prepare the schema for structured addresses. No legacy
-- column is dropped, renamed or rewritten, and no data is parsed or backfilled in
-- this phase. Existing `address` / `owner_address` fields remain the source of
-- truth until a later backend phase writes the structured fields.
--
-- Party A, Party B and Article 1 snapshots on rental_contracts are stored
-- separately so historical contracts keep the wording they were signed with.
-- owner_settings is deliberately untouched: its contract-party address still
-- lives inside the `settings` JSON document.

ALTER TABLE tenants
    ADD COLUMN house_number VARCHAR(100) NULL AFTER address,
    ADD COLUMN street_number VARCHAR(100) NULL AFTER house_number,
    ADD COLUMN sangkat VARCHAR(150) NULL AFTER street_number,
    ADD COLUMN khan VARCHAR(150) NULL AFTER sangkat,
    ADD COLUMN city_province VARCHAR(150) NULL AFTER khan;

ALTER TABLE houses
    ADD COLUMN house_number VARCHAR(100) NULL AFTER address,
    ADD COLUMN street_number VARCHAR(100) NULL AFTER house_number,
    ADD COLUMN sangkat VARCHAR(150) NULL AFTER street_number,
    ADD COLUMN khan VARCHAR(150) NULL AFTER sangkat;

ALTER TABLE rental_contracts
    ADD COLUMN owner_house_number VARCHAR(100) NULL AFTER owner_address,
    ADD COLUMN owner_street_number VARCHAR(100) NULL AFTER owner_house_number,
    ADD COLUMN owner_sangkat VARCHAR(150) NULL AFTER owner_street_number,
    ADD COLUMN owner_khan VARCHAR(150) NULL AFTER owner_sangkat,
    ADD COLUMN owner_city_province VARCHAR(150) NULL AFTER owner_khan,
    ADD COLUMN tenant_house_number VARCHAR(100) NULL AFTER owner_city_province,
    ADD COLUMN tenant_street_number VARCHAR(100) NULL AFTER tenant_house_number,
    ADD COLUMN tenant_sangkat VARCHAR(150) NULL AFTER tenant_street_number,
    ADD COLUMN tenant_khan VARCHAR(150) NULL AFTER tenant_sangkat,
    ADD COLUMN tenant_city_province VARCHAR(150) NULL AFTER tenant_khan,
    ADD COLUMN house_house_number VARCHAR(100) NULL AFTER tenant_city_province,
    ADD COLUMN house_street_number VARCHAR(100) NULL AFTER house_house_number,
    ADD COLUMN house_sangkat VARCHAR(150) NULL AFTER house_street_number,
    ADD COLUMN house_khan VARCHAR(150) NULL AFTER house_sangkat,
    ADD COLUMN house_city_province VARCHAR(150) NULL AFTER house_khan;
