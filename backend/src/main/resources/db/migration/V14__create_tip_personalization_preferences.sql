-- Day 36: Smart Tip Personalization Preferences Table
-- Stores per-user, per-currency personalization enable/disable state.
-- Missing rows default to personalization enabled (handled at application level).

CREATE TABLE tip_personalization_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    personalization_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_personalization_user_currency UNIQUE (user_id, currency)
);

-- Performance & isolation index
CREATE INDEX idx_personalization_pref_user_currency
ON tip_personalization_preferences (user_id, currency);
