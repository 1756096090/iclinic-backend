-- =====================================================
-- 3. USERS (Staff)
-- =====================================================
INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) 
VALUES (1, 'Dr. Juan', 'García López', 'juan.garcia@clinica.ec', 'hashed_password_123', '+593987654321', 'ADMIN', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1718888888');

INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) 
VALUES (2, 'Dra. María', 'Rodríguez Pérez', 'maria.rodriguez@clinica.ec', 'hashed_password_456', '+593999876543', 'DENTIST', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1799999999');

INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) 
VALUES (3, 'Carlos', 'Mendoza Silva', 'carlos.mendoza@clinica.ec', 'hashed_password_789', '+593998765432', 'RECEPTIONIST', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1705550000');

INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) 
VALUES (4, 'Dr. Pedro', 'Martínez López', 'pedro.martinez@clinica.co', 'hashed_password_col1', '+573001234567', 'ADMIN', 'CEDULA_CO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'COLOMBIAN', 2, 3, '79876543');

-- Reset H2 sequences for next identities
ALTER SEQUENCE users_seq RESTART WITH 5;
