-- ==============================================================
-- V1: Create Users Table
-- ==============================================================
-- WHY UUID primary key?
--   - Prevents sequential ID enumeration attacks
--   - Safe for distributed systems and future microservice split
--   - gen_random_uuid() is native PostgreSQL (no extension needed in PG 13+)
--
-- WHY separate created_at / updated_at?
--   - Audit trail: know when a user registered and last modified
--   - updated_at auto-updates via trigger (common PostgreSQL pattern)
--
-- WHY UNIQUE constraint on email?
--   - Emails are the login identifier — must be unique
--   - Index is created automatically by the UNIQUE constraint
-- ==============================================================

CREATE TABLE users (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ==============================================================
-- Auto-update updated_at on row modification
-- WHY a trigger?
--   - Guarantees updated_at is always correct regardless of
--     whether the application remembers to set it
--   - Defense-in-depth: application also sets it via @PreUpdate
-- ==============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
