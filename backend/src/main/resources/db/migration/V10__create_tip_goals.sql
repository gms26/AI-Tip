CREATE TABLE tip_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    goal_type VARCHAR(50) NOT NULL,
    target_value NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3),
    period VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    restaurant_name VARCHAR(255),
    service_quality VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_tip_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_tip_goals_user_id ON tip_goals(user_id);
CREATE INDEX idx_tip_goals_status ON tip_goals(status);

-- Partial unique index to prevent duplicate ACTIVE goals with the same configuration
CREATE UNIQUE INDEX idx_tip_goals_unique_active 
ON tip_goals (
    user_id, 
    goal_type, 
    COALESCE(currency, ''), 
    period, 
    COALESCE(restaurant_name, ''), 
    COALESCE(service_quality, '')
) 
WHERE status = 'ACTIVE';
