ALTER TABLE iam_db.tenants
    ADD COLUMN contact_email VARCHAR(255),
    ADD COLUMN tax_code VARCHAR(50);
