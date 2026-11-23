-- V70: Widen narrow integer columns that back Java Integer entity fields.
-- These columns were declared TINYINT/SMALLINT, but the entities map them as
-- java.lang.Integer. Hibernate (MySQLDialect) expects "integer" for Integer fields
-- and rejects narrower integer types under spring.jpa.hibernate.ddl-auto=validate,
-- which prevented the application from starting on a Flyway-provisioned database.
--   * invoices.billing_month    TINYINT  -> INT (1..12 enforced by chk_invoices_month)
--   * invoices.billing_year     SMALLINT -> INT
--   * rental_contracts.due_day  SMALLINT -> INT (DEFAULT 5 preserved)
ALTER TABLE invoices
    MODIFY COLUMN billing_month INT NOT NULL,
    MODIFY COLUMN billing_year INT NOT NULL;

ALTER TABLE rental_contracts
    MODIFY COLUMN due_day INT NOT NULL DEFAULT 5;
