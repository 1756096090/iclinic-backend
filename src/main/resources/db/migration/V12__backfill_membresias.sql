-- =============================================================================
-- V12 — Las membresías que faltan, de una vez y de forma determinista
--
-- Sustituye a `MembershipBackfillService`, que corría en cada arranque. Dos
-- motivos, y el primero es que ya no puede funcionar:
--
-- 1. Desde que la aplicación opera como `iclinic_app`, sus conexiones están
--    sujetas a RLS. El backfill recorre usuarios de todas las empresas y escribe
--    membresías sin un tenant fijado: la política lo rechaza. Arrancar con el
--    runner puesto deja un `Membership backfill failed on startup` en cada
--    despliegue.
--
-- 2. Aunque funcionara, depender de un reinicio para que un dato exista es
--    frágil: nadie sabe si "ya corrió". Una migración corre una vez, deja rastro
--    en flyway_schema_history y se puede auditar.
--
-- HEURÍSTICAS, las mismas que tenía el servicio, para no cambiar el resultado:
--   - Los SUPER_ADMIN no generan membresía: son administradores de plataforma y
--     no pertenecen a ninguna clínica (ver V10).
--   - El ADMIN existente de cada empresa pasa a ser su OWNER. Es una suposición
--     de migración, no una regla: si una empresa tiene dos ADMIN, los dos quedan
--     como OWNER y alguien tendrá que decidir.
--
-- ROLLBACK: DELETE de las membresías creadas aquí. Se distinguen porque son las
-- únicas cuyo created_at coincide con el de esta migración; si hace falta,
-- mejor restaurar copia.
-- =============================================================================

SET row_security = off;

INSERT INTO company_memberships (user_id, company_id, role, is_owner, active, created_at, updated_at)
SELECT u.id,
       u.company_id,
       u.role,
       u.role = 'ADMIN',
       COALESCE(u.active, false),
       now(),
       now()
  FROM users u
 WHERE u.company_id IS NOT NULL
   AND u.role <> 'SUPER_ADMIN'
   AND NOT EXISTS (SELECT 1 FROM company_memberships m
                    WHERE m.user_id = u.id AND m.company_id = u.company_id);

-- Los SUPER_ADMIN quedan marcados como administradores de plataforma, que es lo
-- que hacía el servicio antes de saltárselos.
UPDATE users
   SET is_platform_admin = true
 WHERE role = 'SUPER_ADMIN'
   AND is_platform_admin IS NOT TRUE;

-- Informe: cuántos quedan sin membresía y sin ser admin de plataforma. NO aborta
-- todavía —esa guardia va en el DROP, cuando la ruta de alta ya exista— pero
-- deja el número en el log de la migración.
DO $$
DECLARE
    rezagados int;
    detalle   text;
BEGIN
    SELECT count(*), string_agg(u.id || ' (' || u.email || ')', ', ' ORDER BY u.id)
      INTO rezagados, detalle
      FROM users u
     WHERE u.is_platform_admin IS NOT TRUE
       AND NOT EXISTS (SELECT 1 FROM company_memberships m WHERE m.user_id = u.id);

    IF rezagados > 0 THEN
        RAISE WARNING 'Quedan % usuarios sin membresia y sin ser admin de plataforma: %',
            rezagados, detalle;
    ELSE
        RAISE NOTICE 'Todos los usuarios tienen membresia o son admin de plataforma';
    END IF;
END $$;
