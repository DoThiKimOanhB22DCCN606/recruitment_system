CREATE TABLE interviews (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    scheduled_date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_minutes INTEGER NOT NULL,
    interview_type VARCHAR(30) NOT NULL,
    location_or_url VARCHAR(500),
    notes VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_interview_duration CHECK (duration_minutes BETWEEN 15 AND 480),
    CONSTRAINT ck_interview_type CHECK (interview_type IN ('IN_PERSON', 'PHONE', 'VIDEO_CALL')),
    CONSTRAINT ck_interview_status CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_interviews_application ON interviews(tenant_id, application_id, scheduled_date);

CREATE TABLE interview_participants (
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    PRIMARY KEY (interview_id, user_id)
);
