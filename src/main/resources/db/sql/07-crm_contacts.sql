-- =====================================================
-- 7. CRM CONTACTS
-- =====================================================
INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) 
VALUES (1, 1, 1, 'Marco Avalos Quezada', '+593987654322', 'marco.avalos@referral.ec', 'WHATSAPP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) 
VALUES (2, 1, 1, 'Sandra Monroy Paredes', '+593988765432', 'sandra.monroy@supplies.ec', 'TELEGRAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) 
VALUES (3, 2, 3, 'Felipe Gómez Hernández', '+573001234568', 'felipe.gomez@events.co', 'WHATSAPP', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO crm_contacts (id, company_id, branch_id, full_name, phone, email, source_channel, active, created_at, updated_at) 
VALUES (4, 2, 3, 'Valentina Cruz López', '+573002345679', 'valentina.cruz@insurance.co', 'WHATSAPP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Reset H2 sequences for next identities
ALTER SEQUENCE crm_contacts_seq RESTART WITH 5;
