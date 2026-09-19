-- =============================================================================
-- V3 — Retirada de las credenciales locales
--
-- Se ejecuta DESPUÉS de haber dado de alta a los usuarios en Keycloak y de haber
-- rellenado `keycloak_user_id` (ver infra/keycloak/migrate-users.md). Por eso va
-- en una migración aparte de V2: entre las dos hay un paso manual.
--
-- Se retiran tres columnas, y las tres son credenciales:
--   external_auth_id  el UID de Firebase
--   auth_provider     el proveedor, que ahora es siempre Keycloak
--   password          hash BCrypt que ya nadie comprueba. Guardar un hash de
--                     contraseña que no se usa para autenticar es exactamente
--                     el tipo de dato que solo puede hacer daño si se filtra.
--
-- ROLLBACK: recrear las tres columnas vacías. Los valores NO se recuperan, y no
-- deben: las contraseñas viven en Keycloak a partir de aquí. Volver atrás implica
-- volver a pedir a los usuarios que la establezcan.
-- =============================================================================

SET row_security = off;

-- Guardia: no se retira el acceso anterior si alguien quedó sin proyectar en
-- Keycloak. Mejor una migración que falla y los lista que un usuario que no
-- puede volver a entrar.
DO $$
DECLARE
    huerfanos int;
    detalle   text;
BEGIN
    SELECT count(*), string_agg(id || ' (' || email || ')', ', ' ORDER BY id)
      INTO huerfanos, detalle
      FROM users
     WHERE keycloak_user_id IS NULL
       AND active = true;

    IF huerfanos > 0 THEN
        RAISE EXCEPTION
            'V3 abortada: % usuarios activos sin keycloak_user_id. Ejecuta antes el alta en Keycloak. Afectados: %',
            huerfanos, detalle;
    END IF;
END $$;

ALTER TABLE users
    DROP COLUMN external_auth_id,
    DROP COLUMN auth_provider,
    DROP COLUMN password;
