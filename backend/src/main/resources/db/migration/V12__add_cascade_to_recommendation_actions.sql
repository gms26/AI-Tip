-- Add ON DELETE CASCADE to recommendation_actions.user_id

ALTER TABLE recommendation_actions DROP CONSTRAINT recommendation_actions_user_id_fkey;

ALTER TABLE recommendation_actions
    ADD CONSTRAINT recommendation_actions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
