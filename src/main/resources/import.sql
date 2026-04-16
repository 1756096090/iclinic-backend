-- =============================================================================
-- import.sql — Datos de prueba para el perfil H2 (desarrollo local)
-- Cargado automáticamente por Hibernate cuando ddl-auto=create-drop|create.
-- IMPORTANTE: cada sentencia debe estar en UNA SOLA LÍNEA para Hibernate 6 / H2.
-- Orden respeta dependencias FK: companies → branches → users →
--   channel_connections → contacts → contact_phones → conversations → messages
-- =============================================================================

-- =====================================================
-- 1. COMPANIES
-- =====================================================
INSERT INTO companies (id, name, company_type, type, ruc, nit) VALUES (1, 'Clínica Dental Premium Ecuador', 'ECUADORIAN', 'ECUADORIAN', '1718888888992', NULL);
INSERT INTO companies (id, name, company_type, type, ruc, nit) VALUES (2, 'Clínica Dental Bogotá', 'COLOMBIAN', 'COLOMBIAN', NULL, '9009034567');

-- =====================================================
-- 2. BRANCHES
-- =====================================================
INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) VALUES (1, 'Sucursal Centro Quito', 'Av. Patria N31-169, Quito', 'CLINIC', 'CLINIC', 1, true);
INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) VALUES (2, 'Sucursal Mariscal Quito', 'Mariscal Sucre y 6 de Diciembre, Quito', 'CLINIC', 'CLINIC', 1, true);
INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory) VALUES (3, 'Sucursal Centro Bogotá', 'Carrera 7 #32-16, Bogotá', 'CLINIC', 'CLINIC', 2, true);

-- =====================================================
-- 3. USERS (Staff)
-- =====================================================
INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) VALUES (1, 'Dr. Juan', 'García López', 'juan.garcia@clinica.ec', 'hashed_password_123', '+593987654321', 'ADMIN', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1718888888');
INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) VALUES (2, 'Dra. María', 'Rodríguez Pérez', 'maria.rodriguez@clinica.ec', 'hashed_password_456', '+593999876543', 'DENTIST', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1799999999');
INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) VALUES (3, 'Carlos', 'Mendoza Silva', 'carlos.mendoza@clinica.ec', 'hashed_password_789', '+593998765432', 'RECEPTIONIST', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1705550000');
INSERT INTO users (id, first_name, last_name, email, password, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number) VALUES (4, 'Dr. Pedro', 'Martínez López', 'pedro.martinez@clinica.co', 'hashed_password_col1', '+573001234567', 'ADMIN', 'CEDULA_CO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'COLOMBIAN', 2, 3, '79876543');

-- =====================================================
-- 4. CHANNEL CONNECTIONS (Meta WhatsApp + Telegram)
-- =====================================================
INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, webhook_registered_url, status, created_at) VALUES (1, 1, 1, 'WHATSAPP', 'META', 'wabiz_ecuador_123', '1234567890123', '{plain}encrypted_token_ec_1', 'verify_token_ec_001', NULL, 'VERIFIED', CURRENT_TIMESTAMP);
INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, webhook_registered_url, status, created_at) VALUES (2, 2, 3, 'WHATSAPP', 'META', 'wabiz_colombia_456', '9876543210987', '{plain}encrypted_token_co_1', 'verify_token_co_001', NULL, 'VERIFIED', CURRENT_TIMESTAMP);
INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, webhook_registered_url, status, created_at) VALUES (3, 1, 1, 'TELEGRAM', 'TELEGRAM', '@Jenkins1234bot', NULL, '{plain}8488844160:AAGHTthIc034kD2_6H2IErpEjMwzVcPzRJ4', NULL, NULL, 'ACTIVE', CURRENT_TIMESTAMP);

-- =====================================================
-- 5. CRM CONTACTS
-- =====================================================
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) VALUES (1, 1, 1, 'Marco Avalos Quezada', '+593987654322', 'marco.avalos@referral.ec', 'WHATSAPP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) VALUES (2, 1, 1, 'Sandra Monroy Paredes', '+593988765432', 'sandra.monroy@supplies.ec', 'TELEGRAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) VALUES (3, 2, 3, 'Felipe Gómez Hernández', '+573001234568', 'felipe.gomez@events.co', 'WHATSAPP', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) VALUES (4, 2, 3, 'Valentina Cruz López', '+573002345679', 'valentina.cruz@insurance.co', 'WHATSAPP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- 6. CONTACT PHONES
-- =====================================================
INSERT INTO crm_contact_phones (id, contact_id, raw_phone, normalized_phone, company_id, created_at) VALUES (1, 1, '+593987654322', '+593987654322', 1, CURRENT_TIMESTAMP);
INSERT INTO crm_contact_phones (id, contact_id, raw_phone, normalized_phone, company_id, created_at) VALUES (2, 2, '+593988765432', '+593988765432', 1, CURRENT_TIMESTAMP);
INSERT INTO crm_contact_phones (id, contact_id, raw_phone, normalized_phone, company_id, created_at) VALUES (3, 3, '+573001234568', '+573001234568', 2, CURRENT_TIMESTAMP);
INSERT INTO crm_contact_phones (id, contact_id, raw_phone, normalized_phone, company_id, created_at) VALUES (4, 4, '+573002345679', '+573002345679', 2, CURRENT_TIMESTAMP);

-- =====================================================
-- 6.5 CHANNEL USER LINKS
-- =====================================================
INSERT INTO crm_channel_user_links (id, contact_id, channel_type, external_user_id, external_chat_id, username, display_name, created_at) VALUES (1, 1, 'WHATSAPP', '593987654322', '593987654322', NULL, 'Marco Avalos Quezada', CURRENT_TIMESTAMP);
INSERT INTO crm_channel_user_links (id, contact_id, channel_type, external_user_id, external_chat_id, username, display_name, created_at) VALUES (2, 3, 'WHATSAPP', '573001234568', '573001234568', NULL, 'Felipe Gómez Hernández', CURRENT_TIMESTAMP);
INSERT INTO crm_channel_user_links (id, contact_id, channel_type, external_user_id, external_chat_id, username, display_name, created_at) VALUES (3, 4, 'WHATSAPP', '573002345679', '573002345679', NULL, 'Valentina Cruz López', CURRENT_TIMESTAMP);

-- =====================================================
-- 7. CRM CONVERSATIONS
-- contact_id 1 → WhatsApp (channel 1)
-- contact_id 2 → Telegram (channel 3)
-- contact_id 3 → WhatsApp Colombia (channel 2)
-- contact_id 4 → WhatsApp Colombia (channel 2)
-- =====================================================
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) VALUES (1, 1, 1, 2, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) VALUES (2, 2, 3, 3, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) VALUES (3, 3, 2, 4, 'CLOSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) VALUES (4, 4, 2, 4, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- 8. CRM MESSAGES
-- =====================================================
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) VALUES (1, 1, 'INBOUND', 'WHATSAPP', 'TEXT', 'RECEIVED', 'msg_ext_001', 'Hola, necesito información sobre servicios dentales', NULL, CURRENT_TIMESTAMP);
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) VALUES (2, 1, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_002', 'Bienvenido, estamos listos para asistirte. ¿Cuál es tu consulta específica?', 2, CURRENT_TIMESTAMP);
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) VALUES (3, 2, 'INBOUND', 'TELEGRAM', 'TEXT', 'RECEIVED', 'msg_ext_003', '¿Cuál es el horario de atención?', NULL, CURRENT_TIMESTAMP);
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) VALUES (4, 3, 'INBOUND', 'WHATSAPP', 'TEXT', 'RECEIVED', 'msg_ext_004', 'Quisiera agendar una cita', NULL, CURRENT_TIMESTAMP);
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) VALUES (5, 4, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_005', 'Gracias por contactarnos. Un asesor se pondrá en contacto pronto.', 4, CURRENT_TIMESTAMP);

-- =====================================================
-- 9. RESET IDENTITY COUNTERS
-- Necesario en H2 cuando se insertan filas con IDs explícitos
-- =====================================================
ALTER TABLE companies ALTER COLUMN id RESTART WITH 3;
ALTER TABLE branches ALTER COLUMN id RESTART WITH 4;
ALTER TABLE users ALTER COLUMN id RESTART WITH 5;
ALTER TABLE crm_channel_connections ALTER COLUMN id RESTART WITH 4;
ALTER TABLE crm_contacts ALTER COLUMN id RESTART WITH 5;
ALTER TABLE crm_contact_phones ALTER COLUMN id RESTART WITH 5;
ALTER TABLE crm_channel_user_links ALTER COLUMN id RESTART WITH 4;
ALTER TABLE crm_conversations ALTER COLUMN id RESTART WITH 5;
ALTER TABLE crm_messages ALTER COLUMN id RESTART WITH 6;

