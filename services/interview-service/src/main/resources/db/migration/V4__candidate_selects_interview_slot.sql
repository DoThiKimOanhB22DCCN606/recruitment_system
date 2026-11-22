ALTER TABLE interviews
    ALTER COLUMN scheduled_date DROP NOT NULL,
    ALTER COLUMN start_time DROP NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(40),
    ADD COLUMN candidate_id UUID,
    ADD COLUMN candidate_selected_slot_index INTEGER;

ALTER TABLE interviews DROP CONSTRAINT ck_interview_status;
ALTER TABLE interviews ADD CONSTRAINT ck_interview_status CHECK (
    status IN ('AWAITING_CANDIDATE_SELECTION', 'SCHEDULED', 'CANCELLED', 'COMPLETED')
);

CREATE TABLE interview_time_slots (
    interview_id UUID NOT NULL REFERENCES interviews(id) ON DELETE CASCADE,
    slot_order INTEGER NOT NULL,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    PRIMARY KEY (interview_id, slot_order),
    CONSTRAINT ck_interview_slot_order CHECK (slot_order >= 0),
    CONSTRAINT ck_interview_slot_range CHECK (end_time > start_time)
);

CREATE INDEX idx_interviews_candidate_date_status
    ON interviews(candidate_id, scheduled_date, status);

DO $$
BEGIN
    IF to_regclass('ats_db.applications') IS NOT NULL THEN
        UPDATE interviews i
        SET candidate_id = a.candidate_id
        FROM ats_db.applications a
        WHERE i.application_id = a.id
          AND i.candidate_id IS NULL;
    END IF;
END $$;
