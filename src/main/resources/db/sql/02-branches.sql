-- =====================================================
-- 2. BRANCHES
-- =====================================================
INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) 
VALUES (1, 'Sucursal Centro Quito', 'Av. Patria N31-169, Quito', 'CLINIC', 'CLINIC', 1, true);

INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) 
VALUES (2, 'Sucursal Mariscal Quito', 'Mariscal Sucre y 6 de Diciembre, Quito', 'CLINIC', 'CLINIC', 1, true);

INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) 
VALUES (3, 'Sucursal Centro Bogotá', 'Carrera 7 #32-16, Bogotá', 'CLINIC', 'CLINIC', 2, true);

-- Reset H2 sequences for next identities
ALTER SEQUENCE branches_seq RESTART WITH 4;
