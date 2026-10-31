CREATE TABLE interview_feedbacks (
    id UUID PRIMARY KEY,
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    feedback VARCHAR(2000) NOT NULL,
    recommendation VARCHAR(30) NOT NULL CHECK (
        recommendation IN ('PROCEED', 'OFFER', 'REJECT', 'SECOND_OPINION')
    ),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_interview_feedback_reviewer UNIQUE (interview_id, reviewer_id)
);

CREATE INDEX idx_interview_feedbacks_tenant_interview
    ON interview_feedbacks (tenant_id, interview_id, created_at DESC);
