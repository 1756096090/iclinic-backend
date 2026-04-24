-- =====================================================
-- 8. ADD SOFT DELETE COLUMN TO CHANNEL CONNECTIONS
-- =====================================================
-- Agregar columna deleted_at para soporte de soft delete
ALTER TABLE crm_channel_connections ADD COLUMN deleted_at TIMESTAMP;

-- Crear índice para mejorar performance en queries de exclusión
CREATE INDEX idx_crm_channel_connections_deleted_at ON crm_channel_connections(deleted_at);

