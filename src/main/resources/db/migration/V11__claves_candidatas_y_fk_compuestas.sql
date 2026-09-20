-- =============================================================================
-- V11 — Que PostgreSQL haga IMPOSIBLE mezclar datos de dos clínicas
--
-- Hasta ahora la integridad entre empresas dependía de que el servicio se
-- acordara de comprobarla. El javadoc de CompanyMembership lo admitía: "La
-- integridad (branch.company == company) se valida en el servicio". Cuando la
-- clave foránea existe, esa validación sobra — y dejarla puesta da a entender
-- que la garantía vive en Java.
--
-- LA REGLA que gobierna todo este fichero: una FK de N columnas necesita un
-- UNIQUE con EXACTAMENTE esas N columnas, en ese orden. Un UNIQUE de 2 no
-- respalda una FK de 3. De ahí que haya claves candidatas que parecen
-- redundantes con la PK: no lo son, son el soporte de la FK compuesta.
--
-- `users` NO entra: su company_id es NULLABLE (un administrador de plataforma no
-- pertenece a ninguna empresa) y con nulos un UNIQUE no impide duplicados ni una
-- FK puede resolverse. Es la consecuencia aceptada de la decisión 10.1. La FK de
-- appointments.doctor_id llegará en el bloque C, contra `practitioners`, que sí
-- es tenant-scoped.
--
-- ROLLBACK: DROP de las constraints en orden inverso (primero las FK, después los
-- UNIQUE que las respaldan) y DROP de las columnas company_id añadidas aquí.
-- =============================================================================

SET row_security = off;

-- ─────────────── 1. Detección de inconsistencias previas ────────────────────
-- Antes de tocar nada. Si ya hay filas cruzadas entre empresas, la migración
-- falla y las lista: son datos reales y los mira una persona, no un script.

DO $$
DECLARE
    cruzadas int;
    detalle  text;
BEGIN
    -- membership_branches: la comprobación que hoy vive en el servicio
    SELECT count(*), string_agg('membership=' || mb.membership_id || ' branch=' || mb.branch_id, ', ')
      INTO cruzadas, detalle
      FROM membership_branches mb
      JOIN company_memberships m ON m.id = mb.membership_id
      JOIN branches b           ON b.id = mb.branch_id
     WHERE m.company_id <> b.company_id;
    IF cruzadas > 0 THEN
        RAISE EXCEPTION 'membership_branches: % filas con sucursal de otra empresa: %', cruzadas, detalle;
    END IF;

    SELECT count(*), string_agg('appointment=' || a.id, ', ')
      INTO cruzadas, detalle
      FROM appointments a JOIN branches b ON b.id = a.branch_id
     WHERE a.company_id <> b.company_id;
    IF cruzadas > 0 THEN
        RAISE EXCEPTION 'appointments: % citas con sucursal de otra empresa: %', cruzadas, detalle;
    END IF;

    SELECT count(*), string_agg('appointment=' || a.id, ', ')
      INTO cruzadas, detalle
      FROM appointments a JOIN crm_contacts c ON c.id = a.contact_id
     WHERE a.company_id <> c.company_id;
    IF cruzadas > 0 THEN
        RAISE EXCEPTION 'appointments: % citas con contacto de otra empresa: %', cruzadas, detalle;
    END IF;
END $$;

-- ─────────────── 2. Claves candidatas ───────────────────────────────────────

ALTER TABLE branches                ADD CONSTRAINT uk_branches_company_id        UNIQUE (company_id, id);
ALTER TABLE company_memberships     ADD CONSTRAINT uk_memberships_company_id     UNIQUE (company_id, id);
ALTER TABLE crm_contacts            ADD CONSTRAINT uk_contacts_company_id        UNIQUE (company_id, id);
ALTER TABLE crm_channel_connections ADD CONSTRAINT uk_channels_company_id        UNIQUE (company_id, id);

-- ─────────────── 3. Denormalizar company_id donde falte ─────────────────────
-- Relleno desde el padre ANTES de poner NOT NULL y la FK.

ALTER TABLE membership_branches   ADD COLUMN company_id bigint;
ALTER TABLE branch_schedules      ADD COLUMN company_id bigint;
ALTER TABLE branch_blocked_slots  ADD COLUMN company_id bigint;
ALTER TABLE crm_conversations     ADD COLUMN company_id bigint;
ALTER TABLE crm_messages          ADD COLUMN company_id bigint;
ALTER TABLE crm_channel_user_links ADD COLUMN company_id bigint;

UPDATE membership_branches mb
   SET company_id = m.company_id
  FROM company_memberships m WHERE m.id = mb.membership_id;

UPDATE branch_schedules bs
   SET company_id = b.company_id
  FROM branches b WHERE b.id = bs.branch_id;

UPDATE branch_blocked_slots bbs
   SET company_id = b.company_id
  FROM branches b WHERE b.id = bbs.branch_id;

UPDATE crm_conversations cv
   SET company_id = c.company_id
  FROM crm_contacts c WHERE c.id = cv.contact_id;

UPDATE crm_messages msg
   SET company_id = cv.company_id
  FROM crm_conversations cv WHERE cv.id = msg.conversation_id;

UPDATE crm_channel_user_links cul
   SET company_id = c.company_id
  FROM crm_contacts c WHERE c.id = cul.contact_id;

-- Si algún relleno dejó nulos, la FK fallaría con un mensaje críptico. Mejor aquí.
DO $$
DECLARE
    t text;
    n int;
BEGIN
    FOREACH t IN ARRAY ARRAY['membership_branches','branch_schedules','branch_blocked_slots',
                             'crm_conversations','crm_messages','crm_channel_user_links']
    LOOP
        EXECUTE format('SELECT count(*) FROM %I WHERE company_id IS NULL', t) INTO n;
        IF n > 0 THEN
            RAISE EXCEPTION '%: % filas sin company_id tras el relleno (huerfanas del padre)', t, n;
        END IF;
    END LOOP;
END $$;

ALTER TABLE membership_branches    ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE branch_schedules       ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE branch_blocked_slots   ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE crm_conversations      ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE crm_messages           ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE crm_channel_user_links ALTER COLUMN company_id SET NOT NULL;

-- Clave candidata de conversaciones, que crm_messages necesita
ALTER TABLE crm_conversations ADD CONSTRAINT uk_conversations_company_id UNIQUE (company_id, id);

-- ─────────────── 4. Las FK compuestas ───────────────────────────────────────

ALTER TABLE membership_branches
    DROP CONSTRAINT IF EXISTS fk_membership_branches_membership,
    DROP CONSTRAINT IF EXISTS fk_membership_branches_branch,
    ADD CONSTRAINT fk_mb_membership FOREIGN KEY (company_id, membership_id)
        REFERENCES company_memberships (company_id, id),
    ADD CONSTRAINT fk_mb_branch FOREIGN KEY (company_id, branch_id)
        REFERENCES branches (company_id, id);

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS fk_appointments_branch,
    DROP CONSTRAINT IF EXISTS fk_appointments_contact,
    ADD CONSTRAINT fk_appt_branch FOREIGN KEY (company_id, branch_id)
        REFERENCES branches (company_id, id),
    ADD CONSTRAINT fk_appt_contact FOREIGN KEY (company_id, contact_id)
        REFERENCES crm_contacts (company_id, id);

ALTER TABLE branch_schedules
    DROP CONSTRAINT IF EXISTS fk_branch_schedules_branch,
    ADD CONSTRAINT fk_bs_branch FOREIGN KEY (company_id, branch_id)
        REFERENCES branches (company_id, id);

ALTER TABLE branch_blocked_slots
    DROP CONSTRAINT IF EXISTS fk_blocked_slots_branch,
    ADD CONSTRAINT fk_bbs_branch FOREIGN KEY (company_id, branch_id)
        REFERENCES branches (company_id, id);

ALTER TABLE crm_conversations
    DROP CONSTRAINT IF EXISTS fk_conversations_contact,
    DROP CONSTRAINT IF EXISTS fk_conversations_channel,
    ADD CONSTRAINT fk_cv_contact FOREIGN KEY (company_id, contact_id)
        REFERENCES crm_contacts (company_id, id),
    ADD CONSTRAINT fk_cv_channel FOREIGN KEY (company_id, channel_connection_id)
        REFERENCES crm_channel_connections (company_id, id);

ALTER TABLE crm_messages
    DROP CONSTRAINT IF EXISTS fk_messages_conversation,
    ADD CONSTRAINT fk_msg_conversation FOREIGN KEY (company_id, conversation_id)
        REFERENCES crm_conversations (company_id, id);

ALTER TABLE crm_channel_user_links
    DROP CONSTRAINT IF EXISTS fk_channel_link_contact,
    ADD CONSTRAINT fk_cul_contact FOREIGN KEY (company_id, contact_id)
        REFERENCES crm_contacts (company_id, id);

-- ─────────────── 5. Índices ─────────────────────────────────────────────────
-- Con RLS, company_id se añade como predicado a TODA consulta sobre estas tablas.
-- Si no encabeza el índice, el filtro se aplica después de leer las filas.

CREATE INDEX idx_appointments_company_branch_start ON appointments (company_id, branch_id, scheduled_start);
CREATE INDEX idx_appointments_company_doctor_start ON appointments (company_id, doctor_id, scheduled_start);
CREATE INDEX idx_appointments_company_contact      ON appointments (company_id, contact_id);
CREATE INDEX idx_conversations_company_contact     ON crm_conversations (company_id, contact_id, status);
CREATE INDEX idx_messages_company_conversation     ON crm_messages (company_id, conversation_id, created_at);
CREATE INDEX idx_cul_company_contact               ON crm_channel_user_links (company_id, contact_id);
CREATE INDEX idx_branch_schedules_company_branch   ON branch_schedules (company_id, branch_id);
CREATE INDEX idx_blocked_slots_company_branch      ON branch_blocked_slots (company_id, branch_id, start_date_time);

-- Los antiguos, que ya no encabezan por company_id, sobran.
DROP INDEX IF EXISTS idx_appointments_branch_start;
DROP INDEX IF EXISTS idx_appointments_doctor_start;
DROP INDEX IF EXISTS idx_appointments_contact;
DROP INDEX IF EXISTS idx_conversations_contact_status;
DROP INDEX IF EXISTS idx_messages_conversation_created;
DROP INDEX IF EXISTS idx_branch_schedules_branch;
DROP INDEX IF EXISTS idx_blocked_slots_branch_start;

-- ─────────────── 6. membership_branches la escribe JPA sin entidad ──────────
-- Es la tabla de unión de una relación @ManyToMany: Hibernate genera el INSERT
-- con las dos columnas que conoce y no hay dónde mapear la tercera. Sin esto,
-- cualquier alta de membresía con sucursales falla con "null value in column
-- company_id" — y lo hace en el backfill de arranque, así que la aplicación no
-- levanta.
--
-- El trigger deriva el valor del padre, que ademas garantiza que no puede quedar
-- incoherente aunque alguien inserte a mano.
--
-- ES TEMPORAL. Se retira en el Bloque D: alli membership_branches gana
-- valid_from, valid_until, revoked_at, revoked_by_user_id y revocation_reason,
-- deja de poder ser la tabla de union de un @ManyToMany y pasa a ser una
-- @Entity propia con su company_id mapeado como cualquier otro campo. Entonces
-- este trigger sobra. Sin esta nota, dentro de seis meses nadie sabria por que
-- existe.

CREATE FUNCTION membership_branches_derivar_empresa() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.company_id IS NULL THEN
        SELECT m.company_id INTO NEW.company_id
          FROM company_memberships m WHERE m.id = NEW.membership_id;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_mb_company BEFORE INSERT OR UPDATE ON membership_branches
    FOR EACH ROW EXECUTE FUNCTION membership_branches_derivar_empresa();
