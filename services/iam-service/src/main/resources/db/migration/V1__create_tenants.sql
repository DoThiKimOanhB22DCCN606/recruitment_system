CREATE TABLE iam_db.tenants (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE iam_db.tenants
    ADD CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE','SUSPENDED'));
