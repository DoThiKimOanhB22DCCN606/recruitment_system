ALTER TABLE iam_db.email_verification_tokens
    ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_evt_user_active_expiry
    ON iam_db.email_verification_tokens(user_id, used, expires_at);
