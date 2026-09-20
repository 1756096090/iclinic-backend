-- =============================================================================
-- V4 — Catálogo de zonas horarias y zona de la sucursal
--
-- Hay una empresa ecuatoriana y una colombiana en la base. En cuanto entre la
-- primera cita real, "las 9:00" dejará de estar definido sin saber de qué
-- sucursal hablamos. Esta columna es lo que lo define.
--
-- El catálogo, en vez de una expresión regular: el patrón que se suele escribir
-- para esto, '^[A-Za-z]+/[A-Za-z_]+$', RECHAZA 'UTC' —que es válida— y ACEPTA
-- 'Marte/Olympus' —que no lo es—. Una FK contra pg_timezone_names no se equivoca
-- en ninguno de los dos sentidos.
--
-- MANTENIMIENTO: `timezones` es una foto de pg_timezone_names en el momento de
-- migrar. Al actualizar tzdata (paquete del sistema o versión de PostgreSQL)
-- pueden aparecer zonas nuevas o desaparecer alguna. Refrescar con:
--     INSERT INTO timezones (name)
--     SELECT name FROM pg_timezone_names
--     ON CONFLICT DO NOTHING;
-- No se borran las que desaparezcan: alguna sucursal podría estar usándolas.
--
-- ROLLBACK:
--     ALTER TABLE branches DROP CONSTRAINT fk_branch_tz;
--     ALTER TABLE branches DROP COLUMN timezone;
--     DROP TABLE timezones;
-- =============================================================================

SET row_security = off;

CREATE TABLE timezones (
    name text PRIMARY KEY
);

COMMENT ON TABLE timezones IS
    'Foto de pg_timezone_names. Refrescar al actualizar tzdata; ver V4.';

INSERT INTO timezones (name)
SELECT name FROM pg_timezone_names;

ALTER TABLE branches ADD COLUMN timezone text;

-- Relleno por país de la empresa a la que pertenece la sucursal. Es el único
-- criterio disponible hoy: no hay ningún dato de ubicación más fino.
UPDATE branches b
   SET timezone = CASE c.company_type
                      WHEN 'ECUADORIAN' THEN 'America/Guayaquil'
                      WHEN 'COLOMBIAN'  THEN 'America/Bogota'
                  END
  FROM companies c
 WHERE c.id = b.company_id;

-- Si apareciera un tipo de empresa nuevo sin zona asignada, mejor que la
-- migración falle aquí que dejar una sucursal sin zona y descubrirlo al agendar.
DO $$
DECLARE
    sin_zona int;
    detalle  text;
BEGIN
    SELECT count(*), string_agg(id || ' (' || name || ')', ', ' ORDER BY id)
      INTO sin_zona, detalle
      FROM branches WHERE timezone IS NULL;

    IF sin_zona > 0 THEN
        RAISE EXCEPTION
            'V4 abortada: % sucursales sin zona horaria asignable por tipo de empresa. Afectadas: %',
            sin_zona, detalle;
    END IF;
END $$;

ALTER TABLE branches
    ALTER COLUMN timezone SET NOT NULL,
    ADD CONSTRAINT fk_branch_tz FOREIGN KEY (timezone) REFERENCES timezones (name);

COMMENT ON COLUMN branches.timezone IS
    'Zona IANA de la sucursal. Define que es "hoy" y que son "las 9:00" aqui. '
    'No usar la del servidor para nada que decida disponibilidad.';
