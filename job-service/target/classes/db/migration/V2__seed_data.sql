INSERT INTO job_categories (name, slug) VALUES
('Software Development', 'software-development'),
('Product Management', 'product-management'),
('Design', 'design');

INSERT INTO jobs (company_id, company_name, category_id, title, description, requirements, benefits, location, salary_min, salary_max, employment_type, experience_level, status, created_by) VALUES
(1, 'TechNova Solutions', 1, 'Senior Java Developer', 'We are looking for a Senior Java Developer.', '5+ years experience in Java.', 'Health insurance, 401k.', 'San Francisco', 120000, 160000, 'FULL_TIME', 'SENIOR', 'PUBLISHED', 1),
(3, 'FinTrust Bank', 2, 'Product Manager', 'Lead our mobile banking app team.', 'Experience in fintech.', 'Remote work options.', 'New York', 100000, 140000, 'FULL_TIME', 'MID', 'PUBLISHED', 3),
(2, 'GreenEarth Energy', 3, 'UX/UI Designer', 'Design next-gen energy dashboards.', 'Portfolio required.', 'Flexible hours.', 'Austin', 80000, 110000, 'FULL_TIME', 'JUNIOR', 'PUBLISHED', 2);

INSERT INTO job_skills (job_id, skill) VALUES
(1, 'Java'), (1, 'Spring Boot'),
(2, 'Product Management'), (2, 'Agile'),
(3, 'Figma'), (3, 'UI Design');

INSERT INTO job_tags (job_id, tag) VALUES
(1, 'Backend'), (1, 'Microservices'),
(2, 'Fintech'),
(3, 'GreenTech');
