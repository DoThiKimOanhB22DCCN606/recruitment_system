-- Add tenant_id and recruiter_id to jobs table
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS recruiter_id UUID;

-- Update any existing records
UPDATE jobs SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

-- Make tenant_id NOT NULL
ALTER TABLE jobs ALTER COLUMN tenant_id SET NOT NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_id ON jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_jobs_recruiter_id ON jobs(recruiter_id);
