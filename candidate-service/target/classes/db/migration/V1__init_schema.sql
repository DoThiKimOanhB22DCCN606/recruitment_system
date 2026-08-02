CREATE TABLE candidates (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE,   -- FK to Auth Service user (cross-service, no DB constraint)
    full_name           VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    phone               VARCHAR(30),
    date_of_birth       DATE,
    gender              VARCHAR(20),
    address             VARCHAR(255),
    headline            VARCHAR(255),
    summary             TEXT,
    avatar_url           VARCHAR(500),
    open_to_work        BOOLEAN NOT NULL DEFAULT FALSE,
    open_to_work_until  TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE candidate_educations (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    school_name     VARCHAR(255) NOT NULL,
    degree          VARCHAR(150),
    field_of_study  VARCHAR(150),
    start_date      DATE,
    end_date        DATE,
    description     TEXT
);

CREATE TABLE candidate_experiences (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    company_name    VARCHAR(255) NOT NULL,
    job_title       VARCHAR(255) NOT NULL,
    start_date      DATE,
    end_date        DATE,
    is_current      BOOLEAN NOT NULL DEFAULT FALSE,
    description     TEXT
);

CREATE TABLE candidate_skills (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    skill_name      VARCHAR(150) NOT NULL,
    proficiency     VARCHAR(30)  -- BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
);

CREATE TABLE candidate_certificates (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    issued_by       VARCHAR(255),
    issued_date     DATE,
    expiry_date     DATE,
    credential_url  VARCHAR(500)
);

CREATE TABLE candidate_languages (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    language        VARCHAR(100) NOT NULL,
    proficiency     VARCHAR(30)
);

CREATE TABLE candidate_portfolio_links (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    title           VARCHAR(255),
    url             VARCHAR(500) NOT NULL
);

CREATE TABLE candidate_cvs (
    id              BIGSERIAL PRIMARY KEY,
    candidate_id    BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    object_key      VARCHAR(500) NOT NULL,   -- MinIO object key within candidate-cvs bucket
    file_size       BIGINT,
    content_type    VARCHAR(100),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_candidates_user_id ON candidates(user_id);
CREATE INDEX idx_candidates_open_to_work ON candidates(open_to_work, open_to_work_until);
CREATE INDEX idx_candidate_skills_candidate_id ON candidate_skills(candidate_id);
CREATE INDEX idx_candidate_cvs_candidate_id ON candidate_cvs(candidate_id);

-- Ensure only one default CV per candidate
CREATE UNIQUE INDEX uq_candidate_default_cv ON candidate_cvs(candidate_id) WHERE is_default = TRUE;
