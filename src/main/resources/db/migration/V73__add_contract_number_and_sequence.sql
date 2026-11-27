-- V73: Automatic Rental Contract Numbering & Sequence Tracking
--
-- Adds a sequential `contract_number` to `rental_contracts` and creates a dedicated
-- `contract_sequences` table to provide concurrency-safe, non-reusable numbering.

ALTER TABLE rental_contracts
    ADD COLUMN contract_number BIGINT NULL AFTER contract_no;

CREATE TABLE contract_sequences (
    sequence_name VARCHAR(50) NOT NULL PRIMARY KEY,
    next_value BIGINT NOT NULL
) ENGINE=InnoDB;

-- Backfill existing contracts with sequential numbers ordered by creation / id
UPDATE rental_contracts rc
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id ASC) AS row_num
    FROM rental_contracts
) seq ON rc.id = seq.id
SET rc.contract_number = seq.row_num;

-- Initialize sequence with the next available number (MAX(contract_number) + 1 or 1)
INSERT INTO contract_sequences (sequence_name, next_value)
SELECT 'RENTAL_CONTRACT', COALESCE(MAX(contract_number), 0) + 1
FROM rental_contracts;
