-- =====================================================
-- 6. CRM MESSAGES (WhatsApp Messages)
-- =====================================================
INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) 
VALUES (1, 1, 'INBOUND', 'WHATSAPP', 'TEXT', 'RECEIVED', 'msg_ext_001', 'Hola, necesito información sobre servicios dentales', NULL, CURRENT_TIMESTAMP);

INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) 
VALUES (2, 1, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_002', 'Bienvenido, estamos listos para asistirte. ¿Cuál es tu consulta específica?', 2, CURRENT_TIMESTAMP);

INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) 
VALUES (3, 2, 'INBOUND', 'WHATSAPP', 'TEXT', 'RECEIVED', 'msg_ext_003', '¿Cuál es el horario de atención?', NULL, CURRENT_TIMESTAMP);

INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) 
VALUES (4, 3, 'INBOUND', 'WHATSAPP', 'TEXT', 'RECEIVED', 'msg_ext_004', 'Quisiera agendar una cita', NULL, CURRENT_TIMESTAMP);

INSERT INTO crm_messages (id, conversation_id, direction, channel_type, message_type, status, external_message_id, content, sent_by_user_id, created_at) 
VALUES (5, 4, 'OUTBOUND', 'WHATSAPP', 'TEXT', 'DELIVERED', 'msg_ext_005', 'Gracias por contactarnos. Un asesor se pondrá en contacto pronto.', 4, CURRENT_TIMESTAMP);

-- Reset H2 sequences for next identities
ALTER SEQUENCE crm_messages_seq RESTART WITH 6;
