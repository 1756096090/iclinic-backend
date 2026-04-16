-- =====================================================
-- 5. CRM CONVERSATIONS (WhatsApp Conversations)
-- =====================================================
INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) 
VALUES (1, 1, 1, 2, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) 
VALUES (2, 2, 1, 3, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) 
VALUES (3, 3, 2, 4, 'CLOSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_conversations (id, contact_id, channel_connection_id, assigned_user_id, status, last_message_at, created_at, updated_at) 
VALUES (4, 4, 2, 4, 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset H2 sequences for next identities
ALTER SEQUENCE crm_conversations_seq RESTART WITH 5;
