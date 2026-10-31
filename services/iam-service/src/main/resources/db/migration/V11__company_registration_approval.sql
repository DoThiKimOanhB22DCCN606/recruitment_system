ALTER TABLE iam_db.users
    ADD COLUMN pending_contact_email VARCHAR(255),
    ADD COLUMN pending_tax_code VARCHAR(50);

ALTER TABLE iam_db.users DROP CONSTRAINT chk_user_status;
ALTER TABLE iam_db.users ADD CONSTRAINT chk_user_status
    CHECK (status IN ('PENDING_VERIFICATION','PENDING_APPROVAL','REJECTED','ACTIVE','LOCKED','DEACTIVATED'));
