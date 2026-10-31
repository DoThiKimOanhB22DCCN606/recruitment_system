-- ============================================================
-- Migration: Add actor_name column to application_stage_history (FR-ATS-15)
-- ============================================================

ALTER TABLE application_stage_history
ADD COLUMN IF NOT EXISTS actor_name VARCHAR(255);
