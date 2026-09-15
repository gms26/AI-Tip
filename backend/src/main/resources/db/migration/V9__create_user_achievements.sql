-- V9__create_user_achievements.sql

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    icon VARCHAR(100),
    requirement_value INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    achievement_id UUID NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);

-- Seed initial achievement catalog deterministically
INSERT INTO achievements (code, name, description, category, icon, requirement_value) VALUES
-- History
('FIRST_TIP', 'First Tip', 'Record your first tip.', 'HISTORY', 'StarIcon', 1),
('TIP_COLLECTOR', 'Tip Collector', 'Record 10 tips.', 'HISTORY', 'CollectionIcon', 10),
('TIP_MASTER', 'Tip Master', 'Record 50 tips.', 'HISTORY', 'MasterIcon', 50),
('TIP_LEGEND', 'Tip Legend', 'Record 100 tips.', 'HISTORY', 'LegendIcon', 100),

-- Consistency
('FIVE_VISITS', 'Five Visits', 'Visit and tip 5 times.', 'CONSISTENCY', 'VisitIcon', 5),
('TWENTY_FIVE_VISITS', 'Regular Customer', 'Visit and tip 25 times.', 'CONSISTENCY', 'RegularIcon', 25),

-- Exploration
('THREE_RESTAURANTS', 'Explorer', 'Tip at 3 unique restaurants.', 'EXPLORATION', 'ExploreIcon', 3),
('TEN_RESTAURANTS', 'Food Explorer', 'Tip at 10 unique restaurants.', 'EXPLORATION', 'FoodExploreIcon', 10),
('THREE_CURRENCIES', 'Currency Explorer', 'Tip using 3 different currencies.', 'EXPLORATION', 'CurrencyIcon', 3),

-- Personalization
('SERVICE_QUALITY_USER', 'Service Observer', 'Rate the service quality of 1 tip.', 'PERSONALIZATION', 'ObserveIcon', 1),
('PERSONALIZED_USER', 'Personalized Tipper', 'Rate the service quality of 5 tips.', 'PERSONALIZATION', 'PersonalIcon', 5),

-- Budget
('BUDGET_SETTER', 'Budget Setter', 'Create your first tip budget.', 'BUDGET', 'BudgetIcon', 1),
('BUDGET_TRACKER', 'Budget Tracker', 'Create a budget and record 5 tips in that currency.', 'BUDGET', 'TrackIcon', 5),

-- Pooling
('TEAM_PLAYER', 'Team Player', 'Finalize a tip pool.', 'POOLING', 'TeamIcon', 1),
('POOL_MASTER', 'Pool Master', 'Finalize 5 tip pools.', 'POOLING', 'PoolMasterIcon', 5),

-- Receipts
('RECEIPT_SCANNER', 'Receipt Scanner', 'Successfully reconcile an OCR receipt.', 'RECEIPTS', 'ReceiptIcon', 1),
('RECEIPT_RECONCILER', 'Receipt Reconciler', 'Successfully reconcile 5 OCR receipts.', 'RECEIPTS', 'ReconcileIcon', 5),

-- Analytics
('ANALYTICS_USER', 'Data Explorer', 'Use the analytics feature.', 'ANALYTICS', 'DataIcon', 1),
('INSIGHTS_USER', 'Insight Seeker', 'Use the AI insights feature.', 'ANALYTICS', 'InsightIcon', 1);
