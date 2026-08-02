CREATE TABLE companies (
    id                  BIGSERIAL PRIMARY KEY,
    owner_user_id       BIGINT NOT NULL,     -- FK to Auth Service user who administers this company
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    industry            VARCHAR(150),
    company_size        VARCHAR(50),
    website             VARCHAR(500),
    email               VARCHAR(255),
    phone               VARCHAR(30),
    logo_object_key     VARCHAR(500),
    tax_code            VARCHAR(50),
    founded_year        INT,
    verified            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE company_locations (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    address         VARCHAR(500) NOT NULL,
    city            VARCHAR(150),
    country         VARCHAR(150),
    is_headquarters BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE company_social_links (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    platform        VARCHAR(50) NOT NULL,   -- FACEBOOK, LINKEDIN, TWITTER, INSTAGRAM, ...
    url             VARCHAR(500) NOT NULL
);

CREATE TABLE company_images (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    object_key      VARCHAR(500) NOT NULL,
    caption         VARCHAR(255),
    uploaded_at     TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_companies_owner_user_id ON companies(owner_user_id);
CREATE INDEX idx_company_locations_company_id ON company_locations(company_id);
CREATE INDEX idx_company_social_links_company_id ON company_social_links(company_id);
CREATE INDEX idx_company_images_company_id ON company_images(company_id);
