-- =============================================================================
-- V5 — Todo instante pasa a timestamptz
--
-- POR QUÉ AHORA. Las columnas que de verdad importan están VACÍAS: cero citas,
-- cero bloqueos de agenda, cero accesos de doctor externo. Convertirlas hoy no
-- interpreta nada. En cuanto entre la primera cita real —y hay una empresa
-- ecuatoriana y una colombiana en la base— habrá husos mezclados y la conversión
-- pasará de gratis a irreversible.
--
-- LA ZONA ESTÁ MEDIDA, NO ELEGIDA. Quedan 15 filas con valor, todas marcas de
-- auditoría del seed del 2026-09-17 escritas en un mismo segundo. Para saber en
-- qué zona se guardaron se compararon dos columnas que escribió la MISMA
-- ejecución con milisegundos de diferencia:
--
--   users.created_at        (naive,  LocalDateTime.now())  2026-09-17 22:51:19.931
--   crm_contacts.created_at (aware,  Instant.now())        2026-09-18 03:51:19.941+00
--
-- El aware renderizado en America/Guayaquil da 22:51:19.941 — la misma hora de
-- pared, 10 ms después. En UTC daría 03:51 del día siguiente. Luego los naive
-- están en America/Guayaquil, que es la zona del servidor que corrió el seed.
--
-- COLUMNAS SIN DATOS (la conversión es puramente estructural):
--   appointments.scheduled_start, scheduled_end
--   branch_blocked_slots.start_date_time, end_date_time
--   external_doctor_patient_access.created_at, expires_at
--   audit_logs.created_at
--
-- COLUMNAS CON DATOS (15 filas, todas marcas de auditoría):
--   users.created_at (5), updated_at
--   company_memberships.created_at (4), updated_at
--   crm_channel_connections.created_at (3), deleted_at
--   crm_channel_user_links.created_at (3)
--
-- NO SE TOCA branch_schedules.start_time / end_time: son `time`, hora de pared.
-- "De 9 a 13" no es un instante; es una hora local que se resuelve contra
-- branches.timezone al generar los huecos. Convertirlas seria el error clasico.
-- Sus created_at/updated_at ya eran timestamptz.
--
-- ROLLBACK: el ALTER inverso, con la MISMA zona.
--     ALTER TABLE <t> ALTER COLUMN <c> TYPE timestamp
--         USING <c> AT TIME ZONE 'America/Guayaquil';
-- =============================================================================

SET row_security = off;

DO $$
DECLARE
    ZONA constant text := 'America/Guayaquil';
    col  record;
BEGIN
    -- En bucle y no a mano: una columna olvidada es un instante que sigue sin
    -- zona, y eso no se nota hasta que dos sucursales discrepan sobre "hoy".
    -- flyway_schema_history es de Flyway, no nuestra.
    FOR col IN
        SELECT c.table_name, c.column_name
          FROM information_schema.columns c
          JOIN information_schema.tables t
            ON t.table_schema = c.table_schema AND t.table_name = c.table_name
         WHERE c.table_schema = 'public'
           AND t.table_type   = 'BASE TABLE'
           AND c.data_type    = 'timestamp without time zone'
           AND c.table_name  <> 'flyway_schema_history'
         ORDER BY c.table_name, c.column_name
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE %L',
            col.table_name, col.column_name, col.column_name, ZONA);
        RAISE NOTICE 'convertida %.% a timestamptz (desde %)',
            col.table_name, col.column_name, ZONA;
    END LOOP;
END $$;

-- Guardia: que no quede ninguna. Si alguien anade una columna naive despues,
-- esto no la ve, pero el test de integracion si.
DO $$
DECLARE
    restantes int;
    detalle   text;
BEGIN
    SELECT count(*), string_agg(c.table_name || '.' || c.column_name, ', ')
      INTO restantes, detalle
      FROM information_schema.columns c
      JOIN information_schema.tables t
        ON t.table_schema = c.table_schema AND t.table_name = c.table_name
     WHERE c.table_schema = 'public'
       AND t.table_type   = 'BASE TABLE'
       AND c.data_type    = 'timestamp without time zone'
       AND c.table_name  <> 'flyway_schema_history';

    IF restantes > 0 THEN
        RAISE EXCEPTION 'V5 incompleta: quedan % columnas sin zona: %', restantes, detalle;
    END IF;
END $$;
