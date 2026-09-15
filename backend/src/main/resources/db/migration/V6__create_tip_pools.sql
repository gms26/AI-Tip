CREATE TABLE tip_pools (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    restaurant_name VARCHAR(255),
    total_tip NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    distribution_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_tip_pool_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE pool_members (
    id UUID PRIMARY KEY,
    pool_id UUID NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    allocation_percentage NUMERIC(10,5) NOT NULL,
    allocated_amount NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_pool_member_pool FOREIGN KEY (pool_id) REFERENCES tip_pools(id) ON DELETE CASCADE
);
