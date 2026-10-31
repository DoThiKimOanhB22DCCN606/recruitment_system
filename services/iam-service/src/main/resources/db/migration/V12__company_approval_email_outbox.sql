CREATE TABLE iam_db.company_approval_emails (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES iam_db.users(id),
    recipient_email VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_company_approval_emails_pending
    ON iam_db.company_approval_emails(created_at) WHERE sent_at IS NULL;
