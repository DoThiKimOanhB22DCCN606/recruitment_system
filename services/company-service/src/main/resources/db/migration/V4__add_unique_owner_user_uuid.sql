DROP INDEX IF EXISTS idx_companies_owner_user_uuid;
CREATE UNIQUE INDEX IF NOT EXISTS uq_companies_owner_user_uuid ON companies(owner_user_uuid);