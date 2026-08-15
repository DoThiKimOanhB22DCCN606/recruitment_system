#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create all schemas for service isolation (ASM-05: single instance, logical isolation)
    CREATE SCHEMA IF NOT EXISTS iam_db;
    CREATE SCHEMA IF NOT EXISTS profile_db;
    CREATE SCHEMA IF NOT EXISTS candidate_db;
    CREATE SCHEMA IF NOT EXISTS company_db;
    CREATE SCHEMA IF NOT EXISTS job_db;
    CREATE SCHEMA IF NOT EXISTS ats_db;
    CREATE SCHEMA IF NOT EXISTS interview_db;
    CREATE SCHEMA IF NOT EXISTS offer_db;

    -- Application user (read/write but not superuser)
    CREATE ROLE rms_app LOGIN PASSWORD '${APP_DB_PASSWORD}';
    GRANT CONNECT ON DATABASE rms TO rms_app;
    GRANT USAGE ON SCHEMA iam_db, offer_db, profile_db, candidate_db, company_db, job_db, ats_db, interview_db TO rms_app;
    
    -- Grant on existing tables
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA iam_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA offer_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA profile_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA candidate_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA company_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA job_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ats_db TO rms_app;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA interview_db TO rms_app;
    
    -- Grant on future Flyway-created tables
    ALTER DEFAULT PRIVILEGES IN SCHEMA iam_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA offer_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA profile_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA candidate_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA company_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA job_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA ats_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;
    ALTER DEFAULT PRIVILEGES IN SCHEMA interview_db GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO rms_app;

    -- Audit log writer (INSERT-only, cannot UPDATE or DELETE audit records)
    CREATE ROLE rms_audit_writer LOGIN PASSWORD '${AUDIT_DB_PASSWORD}';
    GRANT CONNECT ON DATABASE rms TO rms_audit_writer;
    GRANT USAGE ON SCHEMA ats_db TO rms_audit_writer;   -- ATS Service owns audit_logs table
    ALTER DEFAULT PRIVILEGES IN SCHEMA ats_db GRANT INSERT ON TABLES TO rms_audit_writer;
EOSQL
