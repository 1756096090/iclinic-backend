-- =============================================================================
-- V29 — Aislamiento de tenant en el motor
--
-- NUMERADA AL FINAL DEL RANGO DEL BLOQUE (V10–V29) A PROPÓSITO. Una tabla creada
-- después de este script queda sin política, y por tanto con CERO aislamiento,
-- en silencio. La prueba de cobertura de CI es lo que lo detecta; el hueco de
-- numeración es lo que da sitio a las migraciones del bloque sin empujar ésta.
--
-- LO CONTRAINTUITIVO, y es lo que hace que esto funcione o sea decorativo:
-- con FORCE ROW LEVEL SECURITY el PROPIETARIO de la tabla también queda sujeto a
-- la política si no es superusuario. El DDL pasa, pero un backfill de migración
-- afecta a 0 filas SIN DAR ERROR: `UPDATE t SET x=...` → `UPDATE 0`, sin
-- excepción. Corrupción silenciosa en una migración que "pasó".
--   → Flyway corre como iclinic_migrator, CON BYPASSRLS.
--   → Y toda migración que toque datos lleva `SET row_security = off;` al
--     principio, que convierte ese silencio en
--     `ERROR: query would be affected by row-level security policy`.
-- Un superusuario siempre evita RLS: probando como `postgres` esto NO se
-- reproduce, y en producción SÍ ocurre.
--
-- ROLLBACK:
--     DROP POLICY tenant_isolation ON <t>;
--     ALTER TABLE <t> NO FORCE ROW LEVEL SECURITY;
--     ALTER TABLE <t> DISABLE ROW LEVEL SECURITY;
-- en bucle sobre las mismas tablas. Los roles se pueden dejar.
-- =============================================================================

SET row_security = off;

-- ─────────────── 1. Roles ───────────────────────────────────────────────────
-- La distinción entre estos dos es lo que decide si RLS protege algo.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iclinic_app') THEN
        -- Sin BYPASSRLS y sin ser propietario: sujeto a la política, siempre.
        CREATE ROLE iclinic_app NOLOGIN NOINHERIT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iclinic_migrator') THEN
        CREATE ROLE iclinic_migrator NOLOGIN BYPASSRLS;
    END IF;
END $$;

GRANT USAGE ON SCHEMA public TO iclinic_app;
-- Sin DELETE: las bajas son lógicas (deleted_at / active).
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO iclinic_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO iclinic_app;

-- Sin esto, una tabla creada por una migración posterior nace sin permisos para
-- la aplicación y el fallo aparece en tiempo de ejecución, no al migrar.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE ON TABLES TO iclinic_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO iclinic_app;

GRANT ALL ON SCHEMA public TO iclinic_migrator;
GRANT ALL ON ALL TABLES IN SCHEMA public TO iclinic_migrator;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO iclinic_migrator;

-- Para que la aplicacion pueda hacer SET ROLE iclinic_app con el usuario de
-- conexion que tenga. Sin esto, en desarrollo se conecta como `postgres`
-- —superusuario— y RLS no se le aplica: el aislamiento no protegeria nada y
-- todo pareceria correcto. Ver ComprobacionDeAislamiento y TenantAwareDataSource.
GRANT iclinic_app TO CURRENT_USER;

-- ─────────────── 2. La política, en bucle ───────────────────────────────────
-- En bucle y no tabla por tabla: una tabla con company_id sin política tiene
-- cero aislamiento y nadie se entera. Escribirlas a mano garantiza que algún día
-- se olvide una.

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT c.relname
          FROM pg_class c
          JOIN pg_namespace n ON n.oid = c.relnamespace
          JOIN pg_attribute a ON a.attrelid = c.oid
         WHERE n.nspname = 'public'
           AND c.relkind IN ('r', 'p')
           AND a.attname = 'company_id'
           AND a.attnum > 0 AND NOT a.attisdropped
           -- `users` NO es tenant-scoped: su company_id es nullable y se retira
           -- en el bloque siguiente. Ponerle politica dejaria sin ver a los
           -- administradores de plataforma, que no tienen empresa.
           AND c.relname <> 'users'
         ORDER BY 1
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE  ROW LEVEL SECURITY', t);
        EXECUTE format($f$
            CREATE POLICY tenant_isolation ON %I
                USING      (company_id IS NOT DISTINCT FROM
                            nullif(current_setting('iclinic.company_id', true), '')::bigint)
                WITH CHECK (company_id IS NOT DISTINCT FROM
                            nullif(current_setting('iclinic.company_id', true), '')::bigint)
        $f$, t);
        RAISE NOTICE 'RLS activado en %', t;
    END LOOP;
END $$;

-- El `nullif(current_setting(..., true), '')` es lo que hace que FALLE CERRADA:
-- sin variable fijada, el lado derecho es NULL, la comparacion no se cumple para
-- ninguna fila y la consulta devuelve cero. Lo contrario —abrirse cuando falta
-- el contexto— seria un fallo silencioso que solo se nota cuando ya hay datos
-- de otra clinica en pantalla.

COMMENT ON SCHEMA public IS
    'RLS activo. Toda tabla con company_id lleva la politica tenant_isolation. '
    'La aplicacion fija iclinic.company_id por transaccion; sin ella no ve nada.';
