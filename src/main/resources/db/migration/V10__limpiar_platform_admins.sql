-- =============================================================================
-- V10 — Un administrador de plataforma no pertenece a ninguna empresa
--
-- La guardia que precede al futuro DROP de users.company_id encontró una fila:
-- un SUPER_ADMIN con is_platform_admin = true y company_id = 1, sin membresía.
--
-- No era un fallo de la guardia. `MembershipBackfillService` salta a propósito a
-- los SUPER_ADMIN y los marca como administradores de plataforma sin crearles
-- membresía, pero el seed les dejaba la empresa puesta. El resultado es una fila
-- que dice dos cosas contradictorias: "no pertenezco a ninguna empresa" y
-- "pertenezco a la 1".
--
-- Se limpia el dato en vez de relajar la guardia. Una guardia con una excepción
-- —`AND is_platform_admin IS NOT TRUE`— deja de servir: a partir de ahí, un
-- backfill de verdad incompleto podría esconderse detrás de esa bandera.
--
-- El acceso de un administrador de plataforma NO depende de esta columna: viene
-- de is_platform_admin. Se verificó llamando a /api/v1/auth/me después.
--
-- ROLLBACK: no procede. Restaurar el valor sería reintroducir la contradicción.
-- =============================================================================

SET row_security = off;

UPDATE users
   SET company_id = NULL,
       branch_id  = NULL
 WHERE is_platform_admin = true
   AND (company_id IS NOT NULL OR branch_id IS NOT NULL);

-- La misma guardia que va antes del DROP, aquí como comprobación inmediata.
DO $$
DECLARE
    huerfanos int;
    detalle   text;
BEGIN
    SELECT count(*), string_agg(u.id || ' (' || u.email || ')', ', ' ORDER BY u.id)
      INTO huerfanos, detalle
      FROM users u
     WHERE u.company_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM company_memberships m WHERE m.user_id = u.id);

    IF huerfanos > 0 THEN
        RAISE EXCEPTION
            'Quedan % usuarios con company_id y sin membresia: %. El DROP de users.company_id no puede hacerse todavia.',
            huerfanos, detalle;
    END IF;
END $$;
