CREATE TABLE IF NOT EXISTS offer_db.audit_logs (
    id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    offer_id            UUID             NOT NULL,
    tenant_id           UUID             NOT NULL,
    actor_id            UUID             NULL,
    action              VARCHAR(50)      NOT NULL,
    old_status          VARCHAR(50)      NULL,
    new_status          VARCHAR(50)      NOT NULL,
    notes               TEXT             NULL,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_offer_audit_logs_offer_id ON offer_db.audit_logs(offer_id);
CREATE INDEX IF NOT EXISTS idx_offer_audit_logs_tenant_id ON offer_db.audit_logs(tenant_id);
