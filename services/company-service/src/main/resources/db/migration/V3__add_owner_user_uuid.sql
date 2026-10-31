ALTER TABLE companies ADD COLUMN owner_user_uuid UUID;
CREATE INDEX idx_companies_owner_user_uuid ON companies(owner_user_uuid);
