ALTER TABLE candidates ADD COLUMN user_uuid UUID;
CREATE INDEX idx_candidates_user_uuid ON candidates(user_uuid);
