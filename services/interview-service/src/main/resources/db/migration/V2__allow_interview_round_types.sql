ALTER TABLE interviews DROP CONSTRAINT ck_interview_type;
ALTER TABLE interviews ADD CONSTRAINT ck_interview_type CHECK (
    interview_type IN (
        'IN_PERSON', 'PHONE', 'VIDEO_CALL',
        'HR', 'TECHNICAL', 'BEHAVIORAL', 'FINAL'
    )
);
