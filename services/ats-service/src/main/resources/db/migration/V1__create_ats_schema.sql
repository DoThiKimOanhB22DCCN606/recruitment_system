CREATE TABLE pipeline_stages (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    sequence_order INTEGER NOT NULL,
    terminal BOOLEAN NOT NULL DEFAULT FALSE,
    terminal_status VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_stage_job_order UNIQUE (job_id, sequence_order),
    CONSTRAINT uk_stage_job_name UNIQUE (job_id, name),
    CONSTRAINT ck_stage_terminal_status CHECK (terminal_status IS NULL OR terminal_status IN ('HIRED', 'REJECTED'))
);

CREATE TABLE applications (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    job_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    cover_letter VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    current_stage_id UUID NOT NULL REFERENCES pipeline_stages(id),
    rejection_reason VARCHAR(500),
    applied_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_application_job_candidate UNIQUE (job_id, candidate_id),
    CONSTRAINT ck_application_status CHECK (status IN ('ACTIVE', 'HIRED', 'REJECTED', 'WITHDRAWN'))
);

CREATE INDEX idx_applications_tenant_job ON applications(tenant_id, job_id);
CREATE INDEX idx_applications_candidate ON applications(candidate_id, applied_at DESC);

CREATE TABLE application_stage_history (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES applications(id),
    from_stage_id UUID,
    to_stage_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    before_state JSONB,
    after_state JSONB,
    ip_address VARCHAR(255),
    device_info VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_recipient ON notifications(recipient_user_id, created_at DESC);

CREATE TABLE idempotency_keys (
    id VARCHAR(200) PRIMARY KEY,
    scope VARCHAR(100) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
