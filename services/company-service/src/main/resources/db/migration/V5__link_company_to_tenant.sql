ALTER TABLE companies ADD COLUMN tenant_id UUID;

-- Keep one existing company profile per tenant. Prefer the HR admin's profile;
-- other legacy profiles remain untouched so no existing data or logo is deleted.
DO $$
BEGIN
    IF to_regclass('iam_db.users') IS NOT NULL THEN
        WITH ranked AS (
            SELECT c.id, u.tenant_id,
                   ROW_NUMBER() OVER (
                       PARTITION BY u.tenant_id
                       ORDER BY CASE WHEN u.role = 'HR_ADMIN' THEN 0 ELSE 1 END, c.id
                   ) AS row_num
            FROM companies c
            JOIN iam_db.users u ON u.id = c.owner_user_uuid
            WHERE u.tenant_id IS NOT NULL
        )
        UPDATE companies c
        SET tenant_id = ranked.tenant_id
        FROM ranked
        WHERE c.id = ranked.id AND ranked.row_num = 1;
    END IF;
END $$;

CREATE UNIQUE INDEX uq_companies_tenant_id ON companies(tenant_id);
