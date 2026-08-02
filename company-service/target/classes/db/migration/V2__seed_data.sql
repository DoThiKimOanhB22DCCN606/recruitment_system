INSERT INTO companies (owner_user_id, name, description, industry, company_size, website, email, phone, tax_code, founded_year, verified) VALUES
(1, 'TechNova Solutions', 'A leading software development company.', 'Technology', '50-200', 'https://technova.example.com', 'contact@technova.example.com', '+1-555-0100', 'TAX123', 2010, TRUE),
(2, 'GreenEarth Energy', 'Renewable energy startup.', 'Energy', '10-50', 'https://greenearth.example.com', 'info@greenearth.example.com', '+1-555-0200', 'TAX456', 2018, TRUE),
(3, 'FinTrust Bank', 'Digital banking solutions.', 'Finance', '500-1000', 'https://fintrust.example.com', 'support@fintrust.example.com', '+1-555-0300', 'TAX789', 2005, TRUE);

INSERT INTO company_locations (company_id, address, city, country, is_headquarters) VALUES
(1, '123 Tech Lane', 'San Francisco', 'USA', TRUE),
(2, '456 Solar Blvd', 'Austin', 'USA', TRUE),
(3, '789 Wall St', 'New York', 'USA', TRUE);

INSERT INTO company_social_links (company_id, platform, url) VALUES
(1, 'LINKEDIN', 'https://linkedin.com/company/technova'),
(2, 'TWITTER', 'https://twitter.com/greenearth');
