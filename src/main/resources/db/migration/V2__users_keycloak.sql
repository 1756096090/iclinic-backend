-- =============================================================================
-- V2 — `users` pasa de credencial a proyección del sujeto de Keycloak
--
-- La identidad sale de la base de datos. A partir de aquí:
--   Keycloak    = quién eres, con qué credencial y con qué rol general
--   esta tabla  = la proyección local de ese sujeto, para poder enlazarlo con
--                 el dominio (membresías, citas, mensajes)
--
-- Las columnas se añaden aquí y las de Firebase se retiran en V3, en dos pasos
-- separados, para que el despliegue pueda ejecutar el script de alta en Keycloak
-- entre las dos y rellenar `keycloak_user_id` sin quedarse sin `external_auth_id`.
--
-- ROLLBACK: DROP de las cuatro columnas nuevas. No hay pérdida mientras V3 no se
-- haya aplicado.
-- =============================================================================

SET row_security = off;

ALTER TABLE users
    ADD COLUMN keycloak_user_id   uuid,
    ADD COLUMN subject_type       varchar(20),
    ADD COLUMN keycloak_client_id varchar(120),
    -- Interruptor de emergencia. El filtro rechaza (401) todo token cuyo `iat`
    -- sea anterior a esta marca: un UPDATE corta TODAS las sesiones vivas de un
    -- usuario en la siguiente petición, sin guardar estado de sesión en el
    -- backend y sin esperar a que el token expire.
    ADD COLUMN tokens_valid_from  timestamptz NOT NULL DEFAULT now();

-- Todo lo que hay hoy es una persona; las cuentas de servicio se crean después.
UPDATE users SET subject_type = 'HUMAN' WHERE subject_type IS NULL;

ALTER TABLE users
    ALTER COLUMN subject_type SET NOT NULL,
    ADD CONSTRAINT ck_users_subject_type
        CHECK (subject_type IN ('HUMAN', 'SERVICE_ACCOUNT')),
    -- Una cuenta de servicio SIEMPRE tiene cliente de Keycloak; una persona,
    -- nunca. Escrito como igualdad de condiciones y no como dos CHECK sueltos,
    -- para que no pueda cumplirse solo la mitad.
    ADD CONSTRAINT ck_users_service_client
        CHECK ((subject_type = 'SERVICE_ACCOUNT') = (keycloak_client_id IS NOT NULL)),
    ADD CONSTRAINT uk_users_keycloak_user_id UNIQUE (keycloak_user_id);

-- Resolución del sujeto en cada petición autenticada: es la consulta más
-- caliente del sistema.
CREATE INDEX idx_users_keycloak_user_id ON users (keycloak_user_id)
    WHERE keycloak_user_id IS NOT NULL;

COMMENT ON COLUMN users.keycloak_user_id IS
    'El `sub` del token de Keycloak. Fuente de verdad de la identidad.';
COMMENT ON COLUMN users.tokens_valid_from IS
    'Kill-switch de sesiones: se rechaza todo token con iat anterior a esta marca.';
COMMENT ON COLUMN users.role IS
    'OBSOLETA. La sustituye company_membership_roles en el bloque D. El filtro de '
    'autenticación ya no la lee: el rol sale del JWT intersecado con la membresía.';
