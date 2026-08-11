CREATE TABLE offer_db.offers (
    id                  UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id      UUID             NOT NULL,
    tenant_id           UUID             NOT NULL,
    candidate_id        UUID             NOT NULL,
    proposed_salary     DECIMAL(15,2)    NOT NULL,
    currency            CHAR(3)          NOT NULL,
    start_date          DATE             NOT NULL,
    expiry_date         DATE             NOT NULL,
    notes               TEXT             NULL,
    status              VARCHAR(50)      NOT NULL DEFAULT 'DRAFT',
    created_by          UUID             NOT NULL,
    responded_at        TIMESTAMPTZ      NULL,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_offers_application_id ON offer_db.offers(application_id);
CREATE INDEX idx_offers_tenant_id ON offer_db.offers(tenant_id);
CREATE INDEX idx_offers_status ON offer_db.offers(status);
CREATE INDEX idx_offers_candidate_id ON offer_db.offers(candidate_id);

CREATE UNIQUE INDEX idx_offers_unique_active_per_application
    ON offer_db.offers(application_id)
    WHERE status IN ('DRAFT', 'SENT', 'ACCEPTED');

ALTER TABLE offer_db.offers
    ADD CONSTRAINT chk_offer_status
    CHECK (status IN ('DRAFT','SENT','ACCEPTED','DECLINED','EXPIRED','WITHDRAWN'));

ALTER TABLE offer_db.offers
    ADD CONSTRAINT chk_salary_positive
    CHECK (proposed_salary > 0);
