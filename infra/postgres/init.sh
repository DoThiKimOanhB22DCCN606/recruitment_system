#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create all schemas for service isolation
    CREATE SCHEMA IF NOT EXISTS iam_db;
    CREATE SCHEMA IF NOT EXISTS profile_db;
    CREATE SCHEMA IF NOT EXISTS candidate_db;
    CREATE SCHEMA IF NOT EXISTS company_db;
    CREATE SCHEMA IF NOT EXISTS job_db;
    CREATE SCHEMA IF NOT EXISTS ats_db;
    CREATE SCHEMA IF NOT EXISTS interview_db;
    CREATE SCHEMA IF NOT EXISTS offer_db;

    -- Application user (read/write and DDL for Flyway)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rms_app') THEN
            CREATE ROLE rms_app LOGIN PASSWORD '${APP_DB_PASSWORD}';
        END IF;
    END
    \$\$;

    GRANT CONNECT, CREATE ON DATABASE rms TO rms_app;
    GRANT ALL PRIVILEGES ON SCHEMA iam_db, offer_db, profile_db, candidate_db, company_db, job_db, ats_db, interview_db TO rms_app;
    
    -- Grant on existing and future tables/sequences
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA iam_db, offer_db, profile_db, candidate_db, company_db, job_db, ats_db, interview_db TO rms_app;
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA iam_db, offer_db, profile_db, candidate_db, company_db, job_db, ats_db, interview_db TO rms_app;
    
    ALTER DEFAULT PRIVILEGES IN SCHEMA iam_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA offer_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA profile_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA candidate_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA company_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA job_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA ats_db GRANT ALL ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA interview_db GRANT ALL ON TABLES TO rms_app;

    ALTER DEFAULT PRIVILEGES IN SCHEMA iam_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA offer_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA profile_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA candidate_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA company_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA job_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA ats_db GRANT ALL ON SEQUENCES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA interview_db GRANT ALL ON SEQUENCES TO rms_app;

    -- Audit log writer (INSERT-only, cannot UPDATE or DELETE audit records)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'rms_audit_writer') THEN
            CREATE ROLE rms_audit_writer LOGIN PASSWORD '${AUDIT_DB_PASSWORD}';
        END IF;
    END
    \$\$;
    GRANT CONNECT ON DATABASE rms TO rms_audit_writer;
    GRANT USAGE ON SCHEMA ats_db TO rms_audit_writer;
    ALTER DEFAULT PRIVILEGES IN SCHEMA ats_db GRANT INSERT ON TABLES TO rms_audit_writer;
EOSQL
