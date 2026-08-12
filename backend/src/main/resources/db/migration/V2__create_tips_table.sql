-- ==============================================================
-- V2: Create Tips Table
-- ==============================================================
-- WHY NUMERIC(12,2) for monetary values?
--   - DOUBLE/FLOAT causes rounding errors in financial calculations
--   - NUMERIC stores exact decimal values
--   - Scale 2 = cents precision, Precision 12 = up to $9,999,999,999.99
--
-- WHY CHAR(3) for currency?
--   - ISO 4217 currency codes are exactly 3 characters (USD, EUR, INR)
--   - CHAR(3) is more space-efficient and semantically correct
--   - Prevents invalid values like "US Dollar" or empty strings
--
-- WHY ON DELETE CASCADE?
--   - If a user account is deleted, their tip history goes with them
--   - Prevents orphaned tip records
--   - Alternative (SET NULL) would leave anonymous tips — not useful
--
-- WHY index on user_id?
--   - Tips are always queried by user (findByUserId)
--   - Without an index, every query would scan the entire table
--   - The FK constraint alone doesn't guarantee a usable index in all DBs
-- ==============================================================

CREATE TABLE tips (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    restaurant_name     VARCHAR(255),
    bill_amount         NUMERIC(12, 2)  NOT NULL,
    tip_percentage      NUMERIC(5, 2)   NOT NULL,
    tip_amount          NUMERIC(12, 2)  NOT NULL,
    total_amount        NUMERIC(12, 2)  NOT NULL,
    currency            CHAR(3)         NOT NULL DEFAULT 'USD',
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tips_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT chk_bill_positive CHECK (bill_amount > 0),
    CONSTRAINT chk_tip_percentage_range CHECK (tip_percentage >= 0 AND tip_percentage <= 100),
    CONSTRAINT chk_tip_amount_non_negative CHECK (tip_amount >= 0),
    CONSTRAINT chk_total_amount_positive CHECK (total_amount > 0)
);

-- Index for user-scoped queries (most frequent access pattern)
CREATE INDEX idx_tips_user_id ON tips (user_id);

-- Index for sorted queries (user's tips ordered by date)
CREATE INDEX idx_tips_user_id_created_at ON tips (user_id, created_at DESC);

-- Auto-update updated_at trigger (reuses function from V1)
CREATE TRIGGER trg_tips_updated_at
    BEFORE UPDATE ON tips
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
