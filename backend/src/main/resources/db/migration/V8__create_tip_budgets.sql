-- V8: Create tip_budgets table for Day 17 Smart Budget feature
-- Each user can have one budget per currency.

CREATE TABLE tip_budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency        CHAR(3) NOT NULL,
    monthly_limit   NUMERIC(12, 2) NOT NULL,
    warning_threshold NUMERIC(5, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tip_budgets_user_currency UNIQUE (user_id, currency)
);

CREATE INDEX idx_tip_budgets_user_id ON tip_budgets(user_id);
