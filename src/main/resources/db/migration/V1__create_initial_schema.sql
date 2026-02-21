-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_name VARCHAR(50),
    color VARCHAR(7),
    is_default BOOLEAN NOT NULL DEFAULT false,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(name, user_id)
);

-- Budgets table
CREATE TABLE budgets (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('WEEKLY', 'BI_WEEKLY', 'MONTHLY', 'YEARLY')),
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_date_range CHECK (end_date >= start_date)
);

-- Budget allocations table
CREATE TABLE budget_allocations (
    id BIGSERIAL PRIMARY KEY,
    budget_id BIGINT NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    allocated_amount DECIMAL(10, 2) NOT NULL CHECK (allocated_amount >= 0),
    spent_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (spent_amount >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(budget_id, category_id)
);

-- Expenses table
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    transaction_date TIMESTAMP NOT NULL,
    budget_id BIGINT NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Receipts table
CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_url TEXT,
    expense_id BIGINT UNIQUE REFERENCES expenses(id) ON DELETE CASCADE,
    ocr_text TEXT,
    ocr_data JSONB,
    is_processed BOOLEAN NOT NULL DEFAULT false,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_budget_user_id ON budgets(user_id);
CREATE INDEX idx_budget_date_range ON budgets(start_date, end_date);
CREATE INDEX idx_budget_active ON budgets(is_active);
CREATE INDEX idx_expense_budget_id ON expenses(budget_id);
CREATE INDEX idx_expense_category_id ON expenses(category_id);
CREATE INDEX idx_expense_transaction_date ON expenses(transaction_date);
CREATE INDEX idx_budget_allocation_budget_id ON budget_allocations(budget_id);
CREATE INDEX idx_category_user_id ON categories(user_id);