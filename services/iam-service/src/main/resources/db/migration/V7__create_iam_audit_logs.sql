-- ============================================================
-- Migration: Create audit_logs table for iam-service (S.22.5)
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    actor_id UUID,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    ip_address VARCHAR(255),
    device_info VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_iam_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_iam_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_iam_audit_logs_created ON audit_logs(created_at DESC);
