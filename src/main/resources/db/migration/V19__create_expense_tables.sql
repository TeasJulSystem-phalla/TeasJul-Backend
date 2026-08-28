-- Add composite unique key to houses for FK support
-- ALTER TABLE houses
--     ADD UNIQUE KEY uk_houses_id_owner (id, owner_id);

-- Create payment_methods table
CREATE TABLE payment_methods (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_methods_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_payment_methods_id_owner (id, owner_id),
    UNIQUE KEY uk_payment_methods_owner_code (owner_id, code),
    UNIQUE KEY uk_payment_methods_owner_name (owner_id, name),
    INDEX idx_payment_methods_owner_id (owner_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- Create expense_categories table
CREATE TABLE expense_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_expense_categories_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    UNIQUE KEY uk_expense_categories_owner_code (owner_id, code),
    UNIQUE KEY uk_expense_categories_owner_name (owner_id, name),
    UNIQUE KEY uk_expense_categories_id_owner (id, owner_id),
    INDEX idx_expense_categories_owner_id (owner_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- Create expenses table
CREATE TABLE expenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT UNSIGNED NOT NULL,
    expense_no VARCHAR(50) NOT NULL,
    house_id BIGINT UNSIGNED NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_method_id BIGINT UNSIGNED NULL,
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PAID',
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_expenses_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_expenses_house
        FOREIGN KEY (house_id)
        REFERENCES houses(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_expenses_category
        FOREIGN KEY (category_id)
        REFERENCES expense_categories(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_expenses_payment_method
        FOREIGN KEY (payment_method_id)
        REFERENCES payment_methods(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_expenses_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    UNIQUE KEY uk_expenses_owner_no (owner_id, expense_no),
    INDEX idx_expenses_owner_id (owner_id),
    INDEX idx_expenses_house_id (house_id),
    INDEX idx_expenses_category_id (category_id),
    INDEX idx_expenses_date (expense_date)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
