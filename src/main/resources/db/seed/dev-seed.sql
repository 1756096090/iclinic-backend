-- =============================================================================
-- Datos de prueba para desarrollo local sobre PostgreSQL (perfil `dev`).
--
-- NO es una migración de Flyway: se ejecuta por `spring.sql.init` y sólo cuando
-- el perfil `dev` está activo. Es idempotente (ON CONFLICT DO NOTHING), así que
-- puede correr en cada arranque sin duplicar filas.
--
-- Traducción del antiguo import.sql de H2. Diferencias:
--   - ON CONFLICT DO NOTHING en vez de reventar si la fila ya existe.
--   - setval() sobre las secuencias IDENTITY en vez de ALTER ... RESTART WITH.
--   - Los tokens de canal son placeholders. Nunca poner credenciales reales aquí.
-- =============================================================================

-- ─────────────────────────────── Empresas ────────────────────────────────────
INSERT INTO companies (id, name, company_type, type, ruc, nit) VALUES
    (1, 'Clínica Dental Premium Ecuador', 'ECUADORIAN', 'ECUADORIAN', '1718888888992', NULL),
    (2, 'Clínica Dental Bogotá',          'COLOMBIAN',  'COLOMBIAN',  NULL, '9009034567')
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────── Sucursales ──────────────────────────────────
INSERT INTO branches (id, name, address, branch_type, type, company_id, has_laboratory, timezone) VALUES
    (1, 'Sucursal Centro Quito',    'Av. Patria N31-169, Quito',              'CLINIC', 'CLINIC', 1, true, 'America/Guayaquil'),
    (2, 'Sucursal Mariscal Quito',  'Mariscal Sucre y 6 de Diciembre, Quito', 'CLINIC', 'CLINIC', 1, true, 'America/Guayaquil'),
    (3, 'Sucursal Centro Bogotá',   'Carrera 7 #32-16, Bogotá',               'CLINIC', 'CLINIC', 2, true, 'America/Bogota')
ON CONFLICT (id) DO NOTHING;

-- ──────────────────────────────── Usuarios ───────────────────────────────────
-- keycloak_user_id fijo a proposito: son los mismos UUID del realm de desarrollo
-- (infra/keycloak/realm-iclinic.json), para que el seed y Keycloak casen sin
-- pasos manuales. NO hay columna de contrasena: la credencial vive en Keycloak.
INSERT INTO users (id, first_name, last_name, email, phone, role, document_type, active, created_at, updated_at, user_type, company_id, branch_id, document_number, keycloak_user_id, subject_type, tokens_valid_from) VALUES
    (1, 'Dr. Juan',   'García López',      'juan.garcia@clinica.ec',     '+593987654321', 'ADMIN',        'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1718888888', '00000000-0000-4000-8000-000000000001', 'HUMAN', CURRENT_TIMESTAMP),
    (2, 'Dra. María', 'Rodríguez Pérez',   'maria.rodriguez@clinica.ec', '+593999876543', 'DENTIST',      'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1799999999', '00000000-0000-4000-8000-000000000002', 'HUMAN', CURRENT_TIMESTAMP),
    (3, 'Carlos',     'Mendoza Silva',     'carlos.mendoza@clinica.ec',  '+593998765432', 'RECEPTIONIST', 'CEDULA_EC', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', 1, 1, '1705550000', '00000000-0000-4000-8000-000000000003', 'HUMAN', CURRENT_TIMESTAMP),
    (4, 'Dr. Pedro',  'Martínez López',    'pedro.martinez@clinica.co',  '+573001234567', 'ADMIN',        'CEDULA_CO', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'COLOMBIAN',  2, 3, '79876543',   '00000000-0000-4000-8000-000000000004', 'HUMAN', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Administrador de plataforma: SIN empresa ni sucursal. Su acceso viene de
-- is_platform_admin, no de pertenecer a ninguna clinica. Ponerle company_id
-- creaba una fila que se contradecia a si misma; ver V10.
INSERT INTO users (id, first_name, last_name, email, role, document_type, active, is_platform_admin, created_at, updated_at, user_type, company_id, branch_id, keycloak_user_id, subject_type, tokens_valid_from) VALUES
    (5, 'Isaac', '', 'isaaccs2003@gmail.com', 'SUPER_ADMIN', 'PASSPORT', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ECUADORIAN', NULL, NULL, '00000000-0000-4000-8000-000000000005', 'HUMAN', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ───────────────────────── Conexiones de canal ───────────────────────────────
-- El prefijo {plain} lo entiende SecretEncryptionService y evita cifrar en el seed.
INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, webhook_registered_url, status, created_at) VALUES
    (1, 1, 1, 'WHATSAPP', 'META',     'wabiz_ecuador_123',     '1234567890123', '{plain}SEED_PLACEHOLDER_TOKEN_EC', 'verify_token_ec_001', NULL, 'VERIFIED', CURRENT_TIMESTAMP),
    (2, 2, 3, 'WHATSAPP', 'META',     'wabiz_colombia_456',    '9876543210987', '{plain}SEED_PLACEHOLDER_TOKEN_CO', 'verify_token_co_001', NULL, 'VERIFIED', CURRENT_TIMESTAMP),
    (3, 1, 1, 'TELEGRAM', 'TELEGRAM', '@SEED_BOT_PLACEHOLDER', NULL,            '{plain}SEED_PLACEHOLDER_TOKEN_TG', NULL,                  NULL, 'ACTIVE',   CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- ──────────────────────────── Contactos CRM ──────────────────────────────────
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) VALUES
    (1, 1, 1, 'Marco Avalos Quezada',   '+593987654322', 'marco.avalos@referral.ec',    'WHATSAPP', true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 1, 'Sandra Monroy Paredes',  '+593988765432', 'sandra.monroy@supplies.ec',   'TELEGRAM', true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 2, 3, 'Felipe Gómez Hernández', '+573001234568', 'felipe.gomez@events.co',      'WHATSAPP', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 2, 3, 'Valentina Cruz López',   '+573002345679', 'valentina.cruz@insurance.co', 'WHATSAPP', true,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO crm_contact_phones (id, contact_id, raw_phone, normalized_phone, company_id, created_at, updated_at) VALUES
    (1, 1, '+593987654322', '+593987654322', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 2, '+593988765432', '+593988765432', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 3, '+573001234568', '+573001234568', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 4, '+573002345679', '+573002345679', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO crm_channel_user_links (id, contact_id, channel_type, external_user_id, external_chat_id, username, display_name, created_at, company_id) VALUES
    (1, 1, 'WHATSAPP', '593987654322', '593987654322', NULL, 'Marco Avalos Quezada',   CURRENT_TIMESTAMP, 1),
    (2, 3, 'WHATSAPP', '573001234568', '573001234568', NULL, 'Felipe Gómez Hernández', CURRENT_TIMESTAMP, 2),
    (3, 4, 'WHATSAPP', '573002345679', '573002345679', NULL, 'Valentina Cruz López',   CURRENT_TIMESTAMP, 2)
ON CONFLICT (id) DO NOTHING;

-- ────────────────────────── Conversaciones CRM ───────────────────────────────
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at, company_id) VALUES
    (1, 1, 1, 2, 'OPEN',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2, 2, 3, 3, 'OPEN',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (3, 3, 2, 4, 'CLOSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (4, 4, 2, 4, 'OPEN',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2)
ON CONFLICT (id) DO NOTHING;

INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at, updated_at, company_id) VALUES
    (1, 1, 'INBOUND',  'WHATSAPP', 'TEXT', 'RECEIVED',  'msg_ext_001', 'Hola, necesito información sobre servicios dentales',              NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2, 1, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_002', 'Bienvenido, estamos listos para asistirte. ¿Cuál es tu consulta?', 2,    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (3, 2, 'INBOUND',  'TELEGRAM', 'TEXT', 'RECEIVED',  'msg_ext_003', '¿Cuál es el horario de atención?',                                 NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (4, 3, 'INBOUND',  'WHATSAPP', 'TEXT', 'RECEIVED',  'msg_ext_004', 'Quisiera agendar una cita',                                        NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (5, 4, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_005', 'Gracias por contactarnos. Un asesor te contactará pronto.',        4,    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2)
ON CONFLICT (id) DO NOTHING;

-- ──────────────────────── Horarios de doctores ───────────────────────────────
-- Dra. María (id 2) en sucursal 1, L-V 09:00-17:00, slots de 30 min.
-- Dr. Pedro (id 4) en sucursal 3, L-V 10:00-18:00, slots de 30 min.
INSERT INTO branch_schedules (id, branch_id, doctor_id, day_of_week, start_time, end_time, slot_duration_minutes, active, created_at, updated_at, company_id) VALUES
    (1,  1, 2, 'MONDAY',    '09:00', '17:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2,  1, 2, 'TUESDAY',   '09:00', '17:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (3,  1, 2, 'WEDNESDAY', '09:00', '17:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (4,  1, 2, 'THURSDAY',  '09:00', '17:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (5,  1, 2, 'FRIDAY',    '09:00', '17:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (6,  3, 4, 'MONDAY',    '10:00', '18:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (7,  3, 4, 'TUESDAY',   '10:00', '18:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (8,  3, 4, 'WEDNESDAY', '10:00', '18:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (9,  3, 4, 'THURSDAY',  '10:00', '18:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2),
    (10, 3, 4, 'FRIDAY',    '10:00', '18:00', 30, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 2)
ON CONFLICT (id) DO NOTHING;

-- ───────────── Resincronizar secuencias IDENTITY tras IDs explícitos ─────────
SELECT setval(pg_get_serial_sequence('companies', 'id'),               (SELECT COALESCE(MAX(id), 1) FROM companies));
SELECT setval(pg_get_serial_sequence('branches', 'id'),                (SELECT COALESCE(MAX(id), 1) FROM branches));
SELECT setval(pg_get_serial_sequence('users', 'id'),                   (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval(pg_get_serial_sequence('crm_channel_connections', 'id'), (SELECT COALESCE(MAX(id), 1) FROM crm_channel_connections));
SELECT setval(pg_get_serial_sequence('crm_contacts', 'id'),            (SELECT COALESCE(MAX(id), 1) FROM crm_contacts));
SELECT setval(pg_get_serial_sequence('crm_contact_phones', 'id'),      (SELECT COALESCE(MAX(id), 1) FROM crm_contact_phones));
SELECT setval(pg_get_serial_sequence('crm_channel_user_links', 'id'),  (SELECT COALESCE(MAX(id), 1) FROM crm_channel_user_links));
SELECT setval(pg_get_serial_sequence('crm_conversations', 'id'),       (SELECT COALESCE(MAX(id), 1) FROM crm_conversations));
SELECT setval(pg_get_serial_sequence('crm_messages', 'id'),            (SELECT COALESCE(MAX(id), 1) FROM crm_messages));
SELECT setval(pg_get_serial_sequence('branch_schedules', 'id'),        (SELECT COALESCE(MAX(id), 1) FROM branch_schedules));
