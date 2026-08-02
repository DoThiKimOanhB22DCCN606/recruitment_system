-- ============================================================
-- Job Service initial schema
-- ============================================================

CREATE TABLE job_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL,
    company_name        VARCHAR(255),  -- denormalized for full-text search across company name
    category_id         BIGINT REFERENCES job_categories(id),
    title               VARCHAR(255) NOT NULL,
    description         TEXT NOT NULL,
    requirements        TEXT,
    benefits            TEXT,
    location            VARCHAR(255),
    salary_min          NUMERIC(14,2),
    salary_max          NUMERIC(14,2),
    salary_currency     VARCHAR(10) DEFAULT 'VND',
    employment_type     VARCHAR(30) NOT NULL,       -- FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
    experience_level    VARCHAR(30),                -- INTERN, JUNIOR, MID, SENIOR, LEAD
    is_remote           BOOLEAN NOT NULL DEFAULT FALSE,
    vacancies           INT NOT NULL DEFAULT 1,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMP,
    expires_at          TIMESTAMP,
    closed_at           TIMESTAMP,
    created_by          BIGINT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    search_vector       tsvector
);

CREATE TABLE job_skills (
    id      BIGSERIAL PRIMARY KEY,
    job_id  BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill   VARCHAR(100) NOT NULL
);

CREATE TABLE job_tags (
    id      BIGSERIAL PRIMARY KEY,
    job_id  BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    tag     VARCHAR(100) NOT NULL
);

CREATE TABLE job_status_history (
    id             BIGSERIAL PRIMARY KEY,
    job_id         BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    from_status    VARCHAR(30),
    to_status      VARCHAR(30) NOT NULL,
    changed_by     BIGINT,
    note           VARCHAR(500),
    changed_at     TIMESTAMP NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_company ON jobs(company_id);
CREATE INDEX idx_jobs_category ON jobs(category_id);
CREATE INDEX idx_jobs_location ON jobs(location);
CREATE INDEX idx_jobs_expires_at ON jobs(expires_at);
CREATE INDEX idx_job_skills_job_id ON job_skills(job_id);
CREATE INDEX idx_job_skills_skill ON job_skills(skill);

-- Full text search (GIN index on tsvector)
CREATE INDEX idx_jobs_search_vector ON jobs USING GIN (search_vector);

-- Trigger function to keep search_vector up to date
CREATE OR REPLACE FUNCTION jobs_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('simple', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.requirements, '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(NEW.company_name, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.location, '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_jobs_search_vector
    BEFORE INSERT OR UPDATE ON jobs
    FOR EACH ROW EXECUTE FUNCTION jobs_search_vector_update();
