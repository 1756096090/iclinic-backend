-- =====================================================
-- 1. COMPANIES
-- =====================================================
INSERT INTO companies (id, name, company_type, type, ruc, nit) 
VALUES (1, 'Clínica Dental Premium Ecuador', 'ECUADORIAN', 'ECUADORIAN', '1718888888992', NULL);

INSERT INTO companies (id, name, company_type, type, ruc, nit) 
VALUES (2, 'Clínica Dental Bogotá', 'COLOMBIAN', 'COLOMBIAN', NULL, '9009034567');

-- Reset H2 sequences for next identities
ALTER SEQUENCE companies_seq RESTART WITH 3;
