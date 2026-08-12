-- ==============================================================
-- V4: Add service_quality to tips table
-- ==============================================================
-- WHY nullable?
--   Historical tips were created before Day 6. Making the column
--   NOT NULL would require a default value for all existing rows,
--   which would invent a rating the user never gave. We preserve
--   the integrity of historical data by allowing NULL.
--
-- WHY VARCHAR(20) instead of a DB-level CHECK constraint?
--   The enum values (POOR, AVERAGE, GOOD, EXCELLENT) are enforced
--   at the application layer by the Java ServiceQuality enum and
--   @Enumerated(EnumType.STRING). VARCHAR(20) is wide enough for
--   all current and likely future values without storing ordinals.
--   Application-level validation is faster to iterate and easier
--   to change than a DB CHECK constraint across environments.
--
-- WHY NOT rewrite V2?
--   Flyway migrations are immutable once applied. The checksum of
--   V2 is already recorded in flyway_schema_history. Altering V2
--   would break all existing deployments.
--
-- WHY EnumType.STRING instead of ORDINAL?
--   STRING stores "EXCELLENT" not "3". If the enum order changes,
--   existing rows remain correct. ORDINAL breaks on reorder.
-- ==============================================================

ALTER TABLE tips
    ADD COLUMN service_quality VARCHAR(20);

-- No NOT NULL constraint — existing rows will have NULL which
-- correctly represents "not rated" for all pre-Day-6 tips.
