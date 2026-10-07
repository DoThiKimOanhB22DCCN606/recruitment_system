INSERT INTO candidates (user_id, full_name, email, phone, headline, summary, open_to_work) VALUES
(101, 'Alice Smith', 'alice@example.com', '+1-555-1001', 'Senior Backend Developer', 'Passionate about scalable systems.', TRUE),
(102, 'Bob Johnson', 'bob@example.com', '+1-555-1002', 'Product Manager', 'Experienced in leading agile teams.', FALSE),
(103, 'Charlie Brown', 'charlie@example.com', '+1-555-1003', 'UX/UI Designer', 'Creating intuitive user experiences.', TRUE);

INSERT INTO candidate_skills (candidate_id, skill_name, proficiency) VALUES
(1, 'Java', 'EXPERT'),
(1, 'Spring Boot', 'ADVANCED'),
(2, 'Agile', 'EXPERT'),
(3, 'Figma', 'ADVANCED');

INSERT INTO candidate_experiences (candidate_id, company_name, job_title, start_date, is_current, description) VALUES
(1, 'OldTech Corp', 'Backend Developer', '2018-06-01', TRUE, 'Developed microservices.'),
(2, 'Startup Inc', 'Product Owner', '2020-01-15', TRUE, 'Managed product backlog.');
