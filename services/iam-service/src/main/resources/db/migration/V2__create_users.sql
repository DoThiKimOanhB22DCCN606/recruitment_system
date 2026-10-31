CREATE TABLE iam_db.users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID         REFERENCES iam_db.tenants(id),
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    role                    VARCHAR(50)  NOT NULL,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    lockout_until           TIMESTAMPTZ  NULL,
    privacy_accepted_at     TIMESTAMPTZ  NOT NULL,
    pending_company_name    VARCHAR(255) NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON iam_db.users(email);
CREATE INDEX idx_users_tenant_id ON iam_db.users(tenant_id);
CREATE INDEX idx_users_tenant_status ON iam_db.users(tenant_id, status);

ALTER TABLE iam_db.users
    ADD CONSTRAINT chk_user_role
    CHECK (role IN ('CANDIDATE','RECRUITER','HR_ADMIN','SYS_ADMIN'));

ALTER TABLE iam_db.users
    ADD CONSTRAINT chk_user_status
    CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','LOCKED','DEACTIVATED'));
