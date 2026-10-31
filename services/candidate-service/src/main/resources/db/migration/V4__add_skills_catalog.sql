CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE
);

INSERT INTO skills (name) VALUES
('Java'),
('Spring Boot'),
('Python'),
('React'),
('TypeScript'),
('JavaScript'),
('Node.js'),
('PostgreSQL'),
('MySQL'),
('Docker'),
('Kubernetes'),
('AWS'),
('Agile'),
('Figma'),
('Git')
ON CONFLICT (name) DO NOTHING;

INSERT INTO skills (name)
SELECT DISTINCT skill_name FROM candidate_skills
ON CONFLICT (name) DO NOTHING;