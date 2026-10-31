-- ============================================================
-- Migration: Convert is_remote (BOOLEAN) to work_type (VARCHAR)
-- Supports 3-state WorkType: ONSITE, HYBRID, REMOTE
-- ============================================================

ALTER TABLE jobs ADD COLUMN IF NOT EXISTS work_type VARCHAR(30);

UPDATE jobs SET work_type = CASE
    WHEN is_remote = TRUE THEN 'REMOTE'
    ELSE 'ONSITE'
END WHERE work_type IS NULL;

ALTER TABLE jobs ALTER COLUMN work_type SET DEFAULT 'ONSITE';
ALTER TABLE jobs ALTER COLUMN work_type SET NOT NULL;

ALTER TABLE jobs DROP COLUMN IF EXISTS is_remote;

CREATE INDEX IF NOT EXISTS idx_jobs_work_type ON jobs(work_type);
