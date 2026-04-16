-- =====================================================
-- 4. CHANNEL CONNECTIONS (Meta WhatsApp Integration)
-- =====================================================
INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, status, created_at) 
VALUES (1, 1, 1, 'WHATSAPP', 'META', 'wabiz_ecuador_123', '1234567890123', '{plain}encrypted_token_ec_1', 'verify_token_ec_001', 'VERIFIED', CURRENT_TIMESTAMP);

INSERT INTO crm_channel_connections (id, company_id, branch_id, channel_type, provider, external_account_id, external_phone_number_id, access_token_encrypted, webhook_verify_token, status, created_at) 
VALUES (2, 2, 3, 'WHATSAPP', 'META', 'wabiz_colombia_456', '9876543210987', '{plain}encrypted_token_co_1', 'verify_token_co_001', 'VERIFIED', CURRENT_TIMESTAMP);

-- Reset H2 sequences for next identities
ALTER SEQUENCE crm_channel_connections_seq RESTART WITH 3;
