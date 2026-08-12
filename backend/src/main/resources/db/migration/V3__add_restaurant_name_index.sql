-- ==============================================================
-- V3: Add composite index for restaurant-specific personalization queries
-- ==============================================================
-- WHY this index?
--   - PersonalizationService queries tips by (user_id, restaurant_name)
--   - Without this index, restaurant lookups would scan the full user partition
--   - Composite index allows efficient filtering on both columns
--
-- WHY a separate migration instead of modifying V2?
--   - Flyway migrations are immutable once applied
--   - V2 has already been executed in existing environments
--   - Adding a new migration preserves Flyway's checksum integrity
--
-- SCALABILITY NOTE:
--   - For small datasets (< 10K tips per user), this index is optional
--   - For production at scale, this index prevents full table scans
--   - restaurant_name is nullable, so NULL values are excluded from lookups
-- ==============================================================

CREATE INDEX idx_tips_user_restaurant ON tips (user_id, restaurant_name);
