-- Day 32: Smart Tip Recommendation Feedback Table
-- Stores explicit user decisions (ACCEPTED, MODIFIED, CUSTOM) relative to advisory recommendations.

CREATE TABLE tip_recommendation_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    currency VARCHAR(3) NOT NULL,
    restaurant_name VARCHAR(255),
    service_quality VARCHAR(30),
    bill_amount NUMERIC(10, 2) NOT NULL,
    suggested_tip_percentage NUMERIC(5, 2),
    chosen_tip_percentage NUMERIC(5, 2) NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    recommendation_type VARCHAR(50),
    difference_percentage_points NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Performance & isolation indexes
CREATE INDEX idx_tip_rec_feedback_user_curr_date
ON tip_recommendation_feedback (user_id, currency, created_at);

CREATE INDEX idx_tip_rec_feedback_user_curr_type
ON tip_recommendation_feedback (user_id, currency, feedback_type);
