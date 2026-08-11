CREATE TABLE iam_db.invitations (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES iam_db.tenants(id),
    invited_email   VARCHAR(255) NOT NULL,
    invited_by      UUID         NOT NULL REFERENCES iam_db.users(id),
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    accepted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inv_token_hash ON iam_db.invitations(token_hash);
