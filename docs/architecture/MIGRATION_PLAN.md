# Plan de migración a la arquitectura objetivo

Estado: borrador para aprobación. No se ha implementado nada.

## 0. Sobre el documento de referencia

El fichero citado en el encargo, `docs/architecture/arquitectura-completa-iclinic.md`,
no existe. En `docs/architecture/` solo está `arquitectura-iclinic.mmd`, un diagrama
ER de 375 líneas con la leyenda embebida en los comentarios de columna.

De las cuatro partes que el encargo describe como contrato, el `.mmd` aporta dos:

| Parte | Presente |
|---|---|
| Diagrama ER | Sí, completo: 40 tablas y sus relaciones. Tras las tres revisiones de §8 son 42 |
| Leyenda de decisiones | Parcial, como comentarios de columna (`"ES la faceta: paciente, doctor"`, `"ancla al humano, no al rol"`, `"define el HOY"`) |
| Constraints | No. Hay pistas en comentarios (`"UNIQUE org+type+value"`, `"EXCLUDE doctor y recurso"`) pero no una especificación |
| Decisiones abiertas | No existe tal sección |

Este plan se construye sobre el `.mmd` más las restricciones del encargo, que
cubren buena parte de lo que faltaría. Donde una u otra cosa no alcanza, hay una
pregunta en §9. Si existe una versión `.md` más completa, este plan debe revisarse
contra ella antes de ejecutar la fase 1.

## 1. Situación actual

19 tablas en `V1__baseline_schema.sql`, 210 clases Java, 25 clases de test.

Lo que el esquema actual no tiene y el objetivo exige: `organizations`, `persons`,
`principals`, `role_assignments`, `access_policies`, `clinical_records`,
`service_types`, `resources`, `appointment_events`, `modules`, campañas, y el
concepto entero de faceta.

Tres rasgos del modelo actual que condicionan todo el trabajo:

1. **`companies` es a la vez tenant y entidad legal.** La herencia
   `Company → EcuadorianCompany | ColombianCompany` mete el RUC y el NIT como
   columnas de la misma tabla. En el objetivo eso se parte en tres niveles.
2. **`users` es a la vez persona, credencial y rol.** Guarda nombre, documento,
   nacionalidad, teléfono, `role` como enum de una sola fila, `company_id` y
   `branch_id` directos.
3. **No existe nada clínico.** `clinical_records` se construye desde cero, lo que
   es una ventaja: no hay datos históricos que migrar ni que auditar
   retroactivamente.

## 2. Mapa tabla por tabla

### 2.1 Tenencia

| Hoy | Objetivo | Conversión |
|---|---|---|
| `companies` | `organizations` + `legal_entities` + `legal_entity_tax_ids` | Cada fila genera una organización, una sociedad y una fila de identificación fiscal. `company_type` EC/CO da `country_code`; `ruc`/`nit` dan `tax_id_type`/`tax_id_value`; `currency_code` y `fiscal_config` no existen hoy |
| `branches.company_id` | `branches.legal_entity_id` | Reapunta a la sociedad creada desde su empresa |
| — | `branches.timezone` | Columna nueva, obligatoria: define el "hoy" |
| — | `resources` | Tabla nueva, sin origen. `branches.has_laboratory` y `bed_capacity` se conservan |

La herencia de `Company` desaparece: no hay subclase por país en el objetivo, solo
`country_code` y `fiscal_config`. Con ella desaparece la cadena
Strategy + Registry + Factory de creación de empresas, que hoy es uno de los
patrones documentados del repositorio.

### 2.2 Persona y acceso

| Hoy | Objetivo | Conversión |
|---|---|---|
| `users` (datos personales) | `persons` | `first_name`, `last_name`, `nationality`. `birth_date` no existe hoy |
| `users.document_type` / `document_number` / `passport_number` | `person_identifiers` | Una fila por documento. `DocumentType` (7 valores con país embebido) se traduce a `id_type` |
| `users.phone` | `person_phones` | Requiere normalización E.164, que hoy no se aplica a usuarios |
| `users.email` | `person_emails` + `users.email` | El correo queda en los dos sitios: como dato de la persona y como credencial |
| `users` (credencial) | `users` + `user_person_links` | Conserva `id`, `email`, `password_hash`, `is_platform_admin`. El enlace a la persona pasa a la tabla nueva, ver 8.1. Pierde `company_id`, `branch_id`, `role`, y todo lo personal |
| `users.external_auth_id` / `auth_provider` | [NECESITA DECISIÓN] | El objetivo muestra `password_hash` pero no la identidad federada. Hoy la autenticación es Firebase. Ver §9.2 |
| `company_memberships` + `membership_branches` | `principals` + `role_assignments` | Cada pertenencia genera un principal por organización y una asignación con `scope_path`. Las sucursales de `membership_branches` generan una asignación por sucursal |
| `roles`, `permissions`, `role_permissions` | `role_definitions` + `permission_catalog` | RBAC clásico por tablas de unión pasa a `permissions jsonb` con `actions`/`notActions` |
| `users.role` (enum de 7 valores) | `role_definitions.code` | Los siete valores se convierten en definiciones de rol de sistema |
| `external_doctor_patient_access` | `role_assignments` | Es un proto-`role_assignment`: ya tiene `reason`, `expires_at`, `active` y ámbito de recurso. Se convierte en asignación con `scope_path` de paciente, `valid_until` y `reason` |
| — | `access_policies` | Tabla nueva. Las ventanas temporales están hoy implícitas en `expires_at` |
| `audit_logs` | `audit_logs` | Gana `organization_id` (hoy `company_id`), `occurred_at`, `actor_assignment_id`, `scope_path`, `ip_address` de tipo `inet`, y `metadata` pasa de `varchar(2000)` a `jsonb`. Ver 8.18: el diagrama objetivo no tenía columna temporal. La tabla actual sí tiene `created_at`, que es de donde sale el backfill |

### 2.3 Agenda y clínico

| Hoy | Objetivo | Conversión |
|---|---|---|
| `appointments.company_id` | `appointments.legal_entity_id` | Reapunta |
| `appointments.contact_id` → `crm_contacts` | `appointments.patient_assignment_id` → `role_assignments` | Cada contacto con cita necesita persona, principal y asignación PATIENT creados en el backfill |
| `appointments.doctor_id` → `users` | `appointments.practitioner_assignment_id` → `role_assignments`, más `practitioner_person_id` → `persons` | Cada doctor con cita necesita asignación DOCTOR. La persona va denormalizada porque el `EXCLUDE` de solapamiento tiene que ir sobre ella, no sobre la asignación, ver 8.19 |
| `appointments.scheduled_start` / `scheduled_end` (`timestamp`) | `scheduled_start` (`timestamptz`) + `slot tstzrange` | Requiere la zona horaria de la sucursal, que hoy no existe. Ver §9.1 |
| — | `appointments.service_type_id`, `resource_id`, `conversation_id`, `attended_at` | Columnas nuevas |
| — | `appointment_events` | Tabla nueva |
| — | `service_types` | Tabla nueva. Hoy la duración sale de `branch_schedules.slot_duration_minutes` |
| `branch_schedules` | `practitioner_schedules` | `doctor_id` pasa a `assignment_id`; gana `valid_from`/`valid_until`. La clave única actual `(doctor_id, day_of_week)` impide horarios versionados y hay que retirarla |
| `branch_blocked_slots` | `schedule_exceptions` | Hoy solo bloquea sucursal entera; el objetivo admite `assignment_id` nulo para ese caso y no nulo para un doctor. Gana `exception_type` |
| — | `clinical_records` | Tabla nueva, sin datos que migrar |

### 2.4 CRM

| Hoy | Objetivo | Conversión |
|---|---|---|
| `crm_channel_connections.company_id` | `legal_entity_id` | El número pertenece a una sociedad |
| `crm_channel_connections.branch_id` | `channel_connection_branches` | De relación única a tabla de unión |
| `webhook_verify_token`, `external_phone_number_id` | `webhook_token uuid UK` | Nuevo identificador por conexión, generado en el backfill |
| `crm_contacts.company_id` | `organization_id` | Reapunta al tenant |
| `crm_contacts.full_name` | `display_name` | Renombrado |
| `crm_contacts.phone`, `email` | `person_phones`, `person_emails` | Solo cuando el contacto se identifica con una persona; `person_id` es nulo hasta entonces |
| `crm_contact_phones` | `person_phones` | La tabla desaparece; los números pasan a la persona |
| `crm_contacts.source_channel`, `branch_id`, `active` | Sin equivalente | `active` se subsume en `status` (LEAD/ACTIVE/ARCHIVED) |
| `crm_channel_user_links` | igual + `organization_id` | La unicidad pasa a ser por organización, ver 8.2. Conserva `external_chat_id`, `username` y `display_name`, ver 8.4 |
| `crm_conversations` | igual + `organization_id`, `team_id`, `window_expires_at` | `status` se conserva con los mismos tres valores, ver 8.4 |
| `crm_messages` | igual + `organization_id` | `status` se sustituye por `crm_message_events`; `media_url` por `crm_message_attachments` |
| — | `teams`, `contact_consents`, `crm_message_events`, `crm_message_attachments` | Tablas nuevas |
| — | `modules`, `company_modules`, `segments`, `message_templates`, `campaigns`, `campaign_channels`, `campaign_recipients` | Tablas nuevas |

### 2.5 Tablas que desaparecen

`companies`, `company_memberships`, `membership_branches`, `roles`, `permissions`,
`role_permissions`, `crm_contact_phones`, `branch_schedules`,
`branch_blocked_slots`, `external_doctor_patient_access`.

## 3. Fases

Una fase por sesión, cada una en su rama, cada una con `./gradlew build` en verde
antes de cerrarse. Ninguna fase deja el repositorio sin compilar.

### Fase 0 — Preparación (rama `arch/fase-0-preparacion`)

Sin cambio funcional. Deja el terreno listo.

- `btree_gist` habilitado: sin esa extensión no existe `EXCLUDE` con `tstzrange`
  más igualdad sobre `bigint`.
- Testcontainers en `build.gradle`, con perfil de integración separado para que
  `./gradlew test` siga corriendo sin Docker.
- Soporte de `jsonb` en las entidades mediante el mapeo nativo de Hibernate 6.
  No requiere librería nueva.
- Conversión de todas las columnas temporales a `timestamptz`. Hoy conviven
  `TIMESTAMP(6)` y `TIMESTAMP WITH TIME ZONE` en la misma base.
- Infraestructura de aislamiento: definición del filtro de Hibernate y del
  interceptor que lo activa, sin aplicarlo aún a ninguna entidad.

Migración: `V2__preparacion_timestamptz_extensiones.sql`.
Backfill: conversión in situ de columnas temporales, asumiendo que los valores
actuales están en la zona horaria del servidor. [NECESITA DECISIÓN, §9.1]
Rollback: `ALTER` inverso a `timestamp`; la extensión se puede dejar.
Endpoints que rompen: ninguno.
Clases que desaparecen: ninguna.

### Fase 1 — Tenencia (rama `arch/fase-1-tenencia`)

- `organizations`, `legal_entities`, `legal_entity_tax_ids`, `resources`.
- `branches.legal_entity_id` y `branches.timezone`.

Migraciones: `V3__tenencia_estructura.sql`, `V4__tenencia_backfill.sql`,
`V5__tenencia_constraints.sql`.

Backfill: una organización y una sociedad por cada fila de `companies`, con el
`slug` derivado del nombre; `tax_id_type` según `company_type`; `country_code` EC o
CO; reapuntado de `branches`. La zona horaria de cada sucursal no se puede deducir
de los datos. [NECESITA DECISIÓN, §9.1]

Endpoints que rompen contrato:

| Antes | Después |
|---|---|
| `POST /api/v1/companies/ecuadorian`, `/colombian` | Alta de organización y de sociedad por separado |
| `GET /api/v1/companies`, `/{id}` | Respuesta sin `ruc`/`nit` directos; la identificación fiscal pasa a una colección |
| `POST /api/v1/branches/clinic/{companyId}`, `/hospital/{companyId}` | El identificador de la ruta pasa a ser el de la sociedad |
| `GET /api/v1/branches/company/{companyId}` | Por sociedad o por organización |

Clases que desaparecen: `Company`, `EcuadorianCompany`, `ColombianCompany`,
`CompanyFactory`, `CompanyCreationStrategy` y sus tres implementaciones,
`CompanyCreationStrategyRegistry`, los repositorios y DTO por país,
`CompanyType`. Aparecen `Organization`, `LegalEntity`, `LegalEntityTaxId`,
`Resource` con sus capas.

Rollback: las tablas nuevas se eliminan y `branches.company_id` se restaura desde
`legal_entities.organization_id`. Documentado en la cabecera de cada migración.

### Fase 2 — Persona (rama `arch/fase-2-persona`)

- `persons`, `person_identifiers`, `person_phones`, `person_emails`,
  `person_addresses`.
- `users` reducido a credencial, con `user_person_links` como enlace a persona.

Migraciones: `V6__persona_estructura.sql`, `V7__persona_backfill.sql`,
`V8__persona_constraints.sql`.

Backfill: una persona por usuario, dentro de la organización que hoy da su
`company_id`. Los usuarios sin empresa (administradores de plataforma) necesitan
tratamiento aparte. Documento y teléfono se extraen a sus tablas; el teléfono
requiere normalización E.164, que hoy no se aplica y puede producir colisiones con
la unicidad nueva.

Endpoints que rompen contrato: `POST /api/v1/users`, `GET /api/v1/users` y
variantes, `GET /api/v1/auth/me`, `POST /api/v1/auth/firebase/sync`,
`POST /api/v1/admin/users/invite`, `POST /api/v1/admin/client-onboarding`. Todos
cambian la forma del cuerpo: los datos personales dejan de estar en el usuario.

Clases que desaparecen: `EcuadorianUser`, `ColombianUser`, `PeruvianUser`,
`InternationalUser`, `UserType`, `DocumentType` en su forma actual. `User` se
reduce.

Rollback: reconstrucción de las columnas de `users` desde `persons` y sus tablas.
Posible solo mientras exista una persona por usuario.

### Fase 3 — Autorización (rama `arch/fase-3-autorizacion`)

El corazón del cambio.

- `principals`, `role_definitions`, `permission_catalog`, `role_assignments`,
  `access_policies`.
- Compilador de condiciones a SQL, con un conjunto cerrado de operadores,
  devolviendo `Specification` para inyectar en el repositorio.
- Resolución de `@policy.*` contra `access_policies` con la precedencia:
  asignación, rol más sociedad, sociedad, organización, código.
- `organization_id` denormalizado en las tablas tenant-scoped y filtro activo.
- Índices de resolución, incluido `scope_path` con `text_pattern_ops`.

Migraciones: `V9__acceso_estructura.sql`, `V10__acceso_catalogo_roles.sql`,
`V11__acceso_backfill_asignaciones.sql`, `V12__acceso_indices.sql`,
`V13__organization_id_denormalizado.sql`.

Backfill: un principal por pareja de persona y organización; una asignación por
cada `company_memberships`, con `scope_path` de organización; una asignación
adicional por cada fila de `membership_branches`, con `scope_path` de sucursal; una
asignación por cada fila de `external_doctor_patient_access`, con `scope_path` de
paciente, `reason` y `valid_until` desde `expires_at`; las siete definiciones de rol
de sistema con sus permisos.

El mapeo de los siete roles actuales a `actions`/`notActions` no se puede deducir
del código: hoy la autorización es una lista de rutas en `SecurityConfig`, no un
catálogo de permisos. [NECESITA DECISIÓN, §9.3]

Endpoints que rompen contrato: `GET /api/v1/auth/me` (el rol único pasa a una lista
de asignaciones), todo `/api/v1/external-doctor-access/**`, y el comportamiento de
autorización de todos los demás, aunque su forma no cambie.

Clases que desaparecen: `CompanyMembership`, `Role`, `Permission` y sus
repositorios, `MembershipBackfillService`, `MembershipBackfillRunner`,
`ExternalDoctorPatientAccess`, `ExternalDoctorAccessService`,
`ExternalDoctorAccessController`, `UserRole`. `CurrentUserService` se reescribe
entera.

Rollback: el más costoso. Las asignaciones creadas a mano durante la fase no
tienen equivalente en el modelo viejo. Documentar que el rollback es hasta el
punto del backfill, no posterior.

### Fase 4 — Agenda (rama `arch/fase-4-agenda`)

- `service_types`, `practitioner_schedules`, `schedule_exceptions`,
  `appointment_events`.
- `appointments` reapuntado a asignaciones, con `slot tstzrange`.
- `EXCLUDE` de solapamiento por doctor y por recurso.
- "Hoy" calculado en la zona horaria de la sucursal.

Migraciones: `V14__agenda_estructura.sql`, `V15__agenda_backfill.sql`,
`V16__agenda_exclude.sql`.

Backfill: asignación PATIENT por cada contacto con citas y DOCTOR por cada usuario
con citas; `slot` desde `scheduled_start` y `scheduled_end`; horarios desde
`branch_schedules` con `valid_from` abierto; excepciones desde
`branch_blocked_slots` con `assignment_id` nulo; un tipo de servicio por defecto por
sociedad, porque hoy no existe el concepto.

Las citas actuales pueden solaparse: nada lo impide hoy salvo una comprobación en
Java que recorre filas. El `EXCLUDE` fallará si hay solapamientos históricos, y hay
que medirlos antes. [NECESITA DECISIÓN, §9.5]

Endpoints que rompen contrato: todos los de `/api/v1/appointments`.
`GET /available-slots` cambia parámetros; `POST` recibe asignaciones en vez de
identificadores de contacto y de doctor, y crea la asignación PATIENT en el mismo
transaccional; `GET /contact/{contactId}` pasa a ser por asignación o por persona.

Clases que desaparecen: `BranchSchedule`, `BranchBlockedSlot` y sus repositorios.
La validación de solapamiento en Java se retira en favor del constraint.

Rollback: restaurar `doctor_id` y `contact_id` desde las asignaciones. Posible
mientras la asignación conserve su origen.

### Fase 5 — Clínico (rama `arch/fase-5-clinico`)

- `clinical_records` anclado en persona y sociedad.
- Auditoría de lectura, no solo de escritura.
- Acceso denegado a recurso clínico devuelve 404.
- Bloqueo de edición tras la firma.

Migraciones: `V17__clinical_records.sql`.
Backfill: ninguno; no hay datos clínicos.
Endpoints: todos nuevos.
Clases que desaparecen: ninguna.
Rollback: eliminar la tabla.

### Fase 6 — CRM (rama `arch/fase-6-crm`)

- Conexiones colgando de la sociedad, con `webhook_token`.
- `channel_connection_branches`, `teams`, `contact_consents`,
  `crm_message_events`, `crm_message_attachments`.
- `crm_contacts.person_id`, `organization_id` en conversaciones y mensajes,
  `window_expires_at`.
- Unicidad de idempotencia de webhooks.

Migraciones: `V18__crm_estructura.sql`, `V19__crm_backfill.sql`,
`V20__crm_constraints.sql`.

Backfill: `webhook_token` generado para cada conexión; `branch_id` actual movido a
la tabla de unión; `organization_id` propagado; `crm_contact_phones` volcado a
`person_phones` creando persona cuando el contacto se pueda identificar;
`crm_messages.status` volcado a un evento inicial; `media_url` a adjunto.

Aquí se resuelve, de paso, lo que documenta `specs/001-canales-multicuenta/`: el
modelo objetivo ya admite varias conexiones por sociedad. Conviene decidir si esa
spec se cierra como absorbida por esta fase.

Endpoints que rompen contrato: todo `/api/v1/crm/channels`, las rutas de webhook
(pasan a llevar `webhook_token`), y las respuestas de conversación y mensaje.

Clases que desaparecen: `CrmContactPhone` y su repositorio, `MetaWebhookController`,
`TelegramWebhookController`, `MetaWebhookService` y su implementación,
`TelegramWebhookService`, `InboundWebhookMessageDto`, `ChannelConnectionStatus` en
su forma actual.

Rollback: documentado por migración; el volcado de teléfonos a persona no es
reversible sin pérdida.

### Fase 7 — Módulos (rama `arch/fase-7-modulos`)

`modules`, `company_modules`, `permission_catalog.module_code`.
Migración: `V21__modulos.sql`. Backfill: catálogo inicial y activación de todos los
módulos existentes para las organizaciones actuales.
Endpoints: nuevos. Clases que desaparecen: ninguna.

### Fase 8 — Campañas (rama `arch/fase-8-campanas`)

`segments`, `message_templates`, `campaigns`, `campaign_channels`,
`campaign_recipients`.
Migración: `V22__campanas.sql`. Backfill: ninguno.
Endpoints: nuevos. Clases que desaparecen: ninguna.

### Fase 9 — Limpieza (rama `arch/fase-9-limpieza`)

Eliminación de las tablas y columnas de compatibilidad que hayan sobrevivido, y de
las rutas marcadas como obsoletas.
Migración: `V23__limpieza.sql`. Rollback: no lo hay; es el punto de no retorno y
debe ejecutarse solo con las fases anteriores estabilizadas.

## 4. Resumen de migraciones

| Fase | Migraciones |
|---|---|
| 0 | V2 |
| 1 | V3, V4, V5 |
| 2 | V6, V7, V8 |
| 3 | V9, V10, V11, V12, V13 |
| 4 | V14, V15, V16 |
| 5 | V17 |
| 6 | V18, V19, V20 |
| 7 | V21 |
| 8 | V22 |
| 9 | V23 |

Cada fase separa estructura, backfill y constraints en migraciones distintas: un
constraint que falla por datos sucios no debe dejar a medias la creación de las
tablas.

## 5. Constraints comprometidos

| Constraint | Fase | Nota |
|---|---|---|
| `EXCLUDE` solapamiento por doctor | 4 | Sobre `practitioner_person_id` y `slot_blocking`, **no sobre la asignación**, ver 8.19. Parcial: excluye los estados cancelado y no presentado, ver 8.20. Requiere `btree_gist` y datos sin solapes previos |
| `EXCLUDE` solapamiento por recurso | 4 | Sobre `resource_id` y `slot`, no `slot_blocking`, ver 8.21. Parcial: recurso no nulo y estado no cancelado |
| `EXCLUDE` solape de horario del mismo doctor | 4 | `practitioner_schedules`, por asignación, día y rango de vigencia, ver 8.27 |
| `UNIQUE` teléfono de persona | 2 | `(organization_id, normalized_phone)`. Recupera `uq_contact_phone_company`, ver 8.22 |
| `UNIQUE` correo de persona | 2 | `(organization_id, email)`, ver 8.22 |
| `UNIQUE` idempotencia de webhook | 6 | `(channel_connection_id, external_message_id)`. Requiere la columna nueva, ver 8.5 |
| `UNIQUE` identidad por canal | 6 | Por organización, tipo y usuario externo, ver 8.2 |
| `UNIQUE` documento de persona | 2 | Por organización, tipo y valor |
| `UNIQUE` identificación fiscal | 1 | Por tipo y valor. Recupera `ruc`/`nit` únicos del esquema actual, ver 8.8 |
| `UNIQUE` `webhook_token` | 6 | |
| `UNIQUE` `user_person_links.person_id` y `(user_id, organization_id)` | 2 | Ver 8.1 y §9.2 |
| `UNIQUE` `role_definitions (organization_id, code)` | 3 | Ver 8.7 |
| `UNIQUE` `access_policies (organization_id, legal_entity_id, role_definition_id, key)` | 3 | `NULLS NOT DISTINCT`: sin esto la precedencia no es determinista, ver 8.6 |
| `UNIQUE` `crm_message_events (message_id, event)` | 6 | Meta reenvía acuses, ver 8.13 |
| `UNIQUE` `message_templates (organization_id, external_name, language_code)` | 8 | Ver 8.11 |
| `UNIQUE` `team_members (team_id, principal_id)` | 3 | Ver 8.10 |
| Índices de resolución de permisos | 3 | Incluye `scope_path` con `text_pattern_ops` |

## 6. Endpoints que rompen contrato, por fase

| Fase | Familias afectadas |
|---|---|
| 1 | `/api/v1/companies/**`, `/api/v1/branches/**` |
| 2 | `/api/v1/users/**`, `/api/v1/auth/**`, `/api/v1/admin/**` |
| 3 | `/api/v1/auth/me`, `/api/v1/external-doctor-access/**`, y la autorización de todos los demás |
| 4 | `/api/v1/appointments/**` |
| 6 | `/api/v1/crm/channels/**`, `/api/v1/crm/webhooks/**`, respuestas de conversación y mensaje |

Ningún endpoint sobrevive intacto a las fases 1 a 4. Conviene decidir de antemano
si hay un frontend consumiendo esta API y si necesita convivencia de versiones.
[NECESITA DECISIÓN, §9.6]

## 7. Dependencias nuevas

| Librería | Para qué | Estado |
|---|---|---|
| `org.testcontainers:postgresql` y `:junit-jupiter` | Tests de integración contra Postgres real | Pedida en el encargo |
| — | `jsonb`, `tstzrange` e `inet` | No requieren librería; `tstzrange` e `inet` sí requieren un tipo propio de Hibernate, escrito a mano |

No propongo ninguna otra. En particular, no hacen falta ni motor de reglas ni
librería de expresiones: el compilador de condiciones tiene un conjunto cerrado de
operadores, que es justamente lo que el encargo exige.

## 8. Correcciones aplicadas al diagrama

Cuatro puntos del diagrama que, leídos con el resto del encargo, resultaban
contradictorios. **Están corregidos en `arquitectura-iclinic.mmd`**, con la
justificación en la cabecera del propio fichero. Quedan aquí documentados porque
cambian el trabajo de las fases 2, 3 y 6.

**8.1. `users.person_id` único impedía trabajar en dos organizaciones.**
`persons` cuelga de `organizations`, así que un humano que trabaje en dos
organizaciones tiene dos personas. Pero `users` era global, con correo único, y
enlazaba a una sola persona. Ese humano no podía autenticarse una vez y actuar en
las dos: la cadena de autorización pasa por `principals`, que sí es por
organización, pero no había camino desde el usuario hasta el segundo principal.

Corregido: `users` pierde `person_id` y aparece `user_person_links`
(`user_id`, `person_id` único, `organization_id`, único por usuario y
organización). Una credencial, N personas, una por organización; y cada persona
con a lo sumo una credencial. Afecta a la fase 2: el backfill crea la fila de
enlace en vez de una columna.

**8.2. `crm_channel_user_links.external_user_id` con unicidad global reintroducía
una fuga entre tenants.** Es el defecto que `specs/001-canales-multicuenta/spec.md`
documenta hoy en el código: si el mismo número escribe a dos organizaciones, la
segunda reutiliza el contacto de la primera.

Corregido: la tabla lleva `organization_id` y la unicidad pasa a ser por
organización, tipo de canal e identificador externo. Afecta a la fase 6.

**8.3. El diagrama no mostraba `organization_id` en varias tablas que el encargo
declara tenant-scoped**: `appointments`, `clinical_records`, `branches`,
`resources`, `service_types`, `role_assignments` y otras. La regla del encargo dice
que toda tabla tenant-scoped lo lleva denormalizado, y que ninguna query puede
depender de que el desarrollador recuerde filtrar.

Corregido: la columna está en las 37 tablas tenant-scoped. Quedan fuera
`organizations` (es el tenant), `users` (credencial global), `modules` y
`permission_catalog` (catálogos globales). Las aristas
`organizations`–tabla no se dibujan: son una veintena que solo añaden ruido, y la
columna es el contrato. Afecta a la fase 3, que ya contemplaba este trabajo.

**8.4. `crm_conversations` perdía `status`, igual que `crm_messages`.** Para los
mensajes es correcto, porque `crm_message_events` lo sustituye. Para las
conversaciones no había sustituto: `window_expires_at` es la ventana de 24 horas,
no dice si el hilo está abierto, y la bandeja lo necesita.

Corregido: `crm_conversations.status` vuelve con `OPEN`, `PENDING`, `CLOSED`, los
mismos valores que hoy. De paso, `crm_channel_user_links` recupera
`external_chat_id`, `username` y `display_name`, que el diagrama no mostraba y el
envío saliente necesita. Afecta a la fase 6.

### 8.b Segunda revisión: constraints que faltaban y aristas sin columna

Aplicada también al `.mmd`. Los puntos 8.5 a 8.8 son correcciones objetivas; los
8.9 a 8.16 resuelven un hueco de modelado de la forma mínima y aditiva, y conviene
confirmarlos antes de la fase que toque cada tabla.

**8.5.** `crm_messages` gana `channel_connection_id`. El `UNIQUE` de idempotencia
decía `conn+id`, pero la conexión solo se alcanzaba por JOIN a través de la
conversación y un `UNIQUE` no atraviesa un JOIN. Sin esa columna el constraint que
exiges como obligatorio era inimplementable. Fase 6.

**8.6.** `access_policies` gana `UNIQUE (organization_id, legal_entity_id,
role_definition_id, key)` con `NULLS NOT DISTINCT`. Sin él podían coexistir dos
filas en el mismo escalón de precedencia con valores distintos de la misma clave, y
la resolución dependía del planificador. Fase 3.

**8.7.** `role_definitions` gana `UNIQUE (organization_id, code)`. Fase 3.

**8.8.** `legal_entity_tax_ids` gana `UNIQUE (tax_id_type, tax_id_value)`. El
esquema actual tiene `companies.ruc` y `companies.nit` únicos; perderlo era una
regresión. Afecta al backfill de la fase 1: si hay dos empresas con el mismo RUC,
la migración falla. Entra en la medición de §9.5.

**8.9.** `principals.team_id`, nullable. El tipo `GROUP` no tenía a qué apuntar.
Fase 3.

**8.10.** `team_members` (equipo, principal). `teams` existía sin miembros, así que
`crm_conversations.team_id` no permitía saber quién atiende un hilo. El miembro es
un principal y no un usuario, para admitir cuentas de servicio. Fase 3, aunque la
tabla no se use hasta la 6.

**8.11.** `message_templates` gana `language_code` y `variables`. Una plantilla de
Meta se identifica por nombre más idioma, y sin las variables no se puede
renderizar. Fase 8.

**8.12.** `clinical_records` gana `amends_record_id`. `locked` impedía editar lo
firmado pero no había forma de enmendarlo. Fase 5.

**8.13.** `crm_message_events` gana `UNIQUE (message_id, event)`. Fase 6.

**8.14.** `contact_consents` gana `channel_type` y `occurred_at`. El
consentimiento se da para un canal concreto y la fecha del cambio es el dato que se
pide si hay reclamación. Fase 6.

**8.15.** `campaigns` gana `timezone`. `quiet_hours` no tenía ancla: la campaña
cuelga de una sociedad que puede tener sucursales en zonas distintas. Fase 8.

**8.16.** `appointments` separa `slot` y `slot_blocking`. `slot` es lo que se pinta
en la agenda; `slot_blocking` incluye `buffer_after_minutes` y es sobre el que va
el `EXCLUDE`. Con un solo rango, o el buffer no se respetaba o la agenda mostraba
huecos falsos. Fase 4.

**8.17.** Retiradas dos aristas que no tenían columna que las respaldase:
`practitioner_schedules` hacia `appointments` y `schedule_exceptions` hacia
`practitioner_schedules`. Son reglas que se validan al agendar, no claves foráneas,
y dibujadas como relación inducían a implementarlas como FK. Quedan documentadas
como comentario en el `.mmd`. Corregida también la cardinalidad de
`crm_messages` hacia `campaign_recipients`, que estaba invertida.

### 8.c Tercera revisión: lo que solo aparece al ejecutar

Repaso a fondo, tabla por tabla. Los diez primeros puntos son errores que no se
ven leyendo el diagrama pero que se manifiestan en producción; están aplicados al
`.mmd`. El último es de índices y no cambia el esquema.

**8.18. `audit_logs` no tenía ninguna columna temporal.** Un registro de auditoría
sin el "cuándo". Gana `occurred_at`. Gana además `actor_assignment_id` y
`scope_path`: en un sistema clínico no basta con saber qué humano leyó una
historia, hace falta saber con qué faceta actuaba y sobre qué ámbito decía tener
permiso. Sin eso, la auditoría no responde a la pregunta que se le va a hacer.
Fase 3.

**8.19. El `EXCLUDE` de solapamiento iba sobre la asignación, no sobre la
persona.** Es el error más caro del diagrama. Un doctor que atiende en dos
sucursales tiene dos `role_assignments`, uno por ámbito. Un `EXCLUDE` sobre
`practitioner_assignment_id` compara cosas distintas: las dos citas del mismo
humano a la misma hora llevan asignaciones diferentes, el constraint no ve el
choque y el doctor queda agendado en dos sitios a la vez. `appointments` gana
`practitioner_person_id` denormalizado y el `EXCLUDE` va sobre él. Fase 4.

**8.20. El `EXCLUDE` no podía excluir las citas canceladas.** Tal como estaba
descrito, cancelar una cita y reagendar en el mismo hueco era imposible: la fila
cancelada seguía ocupando el rango. Tiene que ser un `EXCLUDE` parcial que deje
fuera los estados cancelado y no presentado. Fase 4.

**8.21. El `EXCLUDE` de recurso no va sobre el mismo rango que el de doctor.** El
sillón se libera cuando acaba la cita; el buffer es tiempo del doctor, no del
sillón. Con los dos constraints sobre `slot_blocking` se bloquean sillones que
están libres. El de recurso va sobre `slot`, el de persona sobre `slot_blocking`.
Fase 4.

**8.22. `person_phones` y `person_emails` no tenían ninguna unicidad.** El esquema
actual sí la tiene (`uq_contact_phone_company`). Perderla rompe la deduplicación
de contactos, que es el camino principal por el que se identifica a un paciente
que escribe por WhatsApp: sin ella el mismo número puede colgar de tres personas
distintas y la unificación deja de funcionar. Es una regresión, no una
simplificación. Fase 2, y afecta al backfill, porque la normalización a E.164
puede producir colisiones. Entra en la medición de §9.5.

**8.23. `clinical_records` solo llegaba hasta `legal_entity_id`.** La jerarquía de
ámbitos es organización, sociedad, sucursal. Un doctor con asignación de ámbito
`/org/1/le/3/branch/7` no tenía por dónde filtrar las historias: veía las de toda
la sociedad. Gana `branch_id`. Fase 5.

**8.24. `clinical_records.content` sin versión de esquema ni hash.** Un odontograma
cambia de forma con el tiempo; migrar cinco años de registros sin saber con qué
versión se escribió cada uno es adivinar. Gana `content_schema_version`. Y
`content_hash`: `locked` impedía editar desde la aplicación, no por debajo, y en
un registro firmado la integridad hay que poder demostrarla. Fase 5.

**8.25. `locked` era redundante con `signed_at` y podían contradecirse.** Firmado
es `signed_at IS NOT NULL`. Columna retirada. Fase 5.

**8.26. `role_assignments.active` no decía ni cuándo ni quién revocó.** Conceder
está auditado con `granted_by_user_id` y `reason`; revocar no lo estaba.
Sustituido por `revoked_at`, `revoked_by_user_id` y `revocation_reason`. Vigente
pasa a ser `revoked_at` nulo y ahora dentro de `[valid_from, valid_until)`: una
sola regla en vez de dos que podían discrepar. Fase 3.

**8.27. `practitioner_schedules` no impedía solaparse consigo mismo.** Dos tramos
del mismo doctor, el mismo día, con rangos de vigencia que se pisan. Necesita su
propio `EXCLUDE`. Fase 4.

**8.28. El índice de resolución de permisos estaba pedido al revés.** El encargo
dice `scope_path` con `text_pattern_ops`, y eso sirve para la consulta *hacia
abajo*: "todo lo que cuelga de `/org/1/le/3`", que es la de los listados
administrativos. Pero el chequeo de permiso va *hacia arriba*: dado el recurso
`/org/1/le/3/branch/7`, qué asignaciones lo cubren. Eso no es un `LIKE` con
comodín al final; es una igualdad contra la lista de ancestros del recurso,
calculada en la aplicación, que son cinco como mucho. Y eso quiere un btree normal
sobre `(principal_id, scope_path)`. Hacen falta los dos índices, por motivos
distintos. Si solo se crea el de `text_pattern_ops`, la consulta caliente del
sistema, la que corre en cada petición, no lo usa. Fase 3, sin cambio de esquema.

### 8.d Lo que queda sin resolver y no puedo decidir yo

Siete puntos más del repaso que no son errores de transcripción sino decisiones de
producto o de operación. Están listados en la cabecera del `.mmd` y detallados en
§9.7 a §9.13.

## 9. Decisiones que necesito antes de empezar

No las resuelvo yo. Cada una bloquea la fase indicada.

**9.1. Zonas horarias (bloquea fases 0, 1 y 4).** ¿Qué zona horaria asigno a las
sucursales existentes, y en qué zona están interpretados los `timestamp` sin zona
que hay hoy en `appointments`, `branch_schedules` y `branch_blocked_slots`? Si es
la del servidor, necesito saber cuál era en el momento en que se escribieron.

**9.2. Identidad federada (bloquea fase 2).** El objetivo muestra `password_hash` y
el código usa Firebase con `external_auth_id`. ¿Se mantiene Firebase, se migra a
credenciales propias, o conviven? Y, ligado a 8.1: ¿un humano que trabaja en dos
organizaciones debe poder autenticarse una sola vez?

**9.3. Catálogo de permisos (bloquea fase 3).** Necesito el mapeo de los siete roles
actuales a `actions` y `notActions`, o al menos el criterio. Hoy la autorización
vive como lista de rutas en `SecurityConfig` y no hay catálogo del que derivarlo.
Ligado: qué claves de `access_policies` existen además de
`clinical.access_before`, y cuál es su valor por defecto.

**9.4. Resuelta.** Era la confirmación de 8.2 y 8.4, ya aplicada al diagrama.
Queda como aviso, no como bloqueo: si no estás de acuerdo con alguna de las cuatro
correcciones, hay que revertirla en el `.mmd` antes de la fase correspondiente.

**9.5. Datos existentes (bloquea fases 1, 2, 4 y 6).** ¿Hay algún entorno con datos
reales, o solo local con `db/seed/dev-seed.sql`? De la respuesta depende que los
backfills tengan que ser defensivos o puedan asumir datos limpios. En concreto
necesito saber si hay citas solapadas, teléfonos que colisionarían al normalizar a
E.164, y contactos compartidos entre empresas.

**9.6. Convivencia de versiones de API (bloquea fase 1).** ¿Hay un frontend
consumiendo esta API? Si lo hay, ¿las rutas viejas deben seguir respondiendo
durante la migración, o se coordina un corte?

**9.7. Facturación (bloquea fase 1).** El encargo dice que de la sociedad cuelgan
"sucursales, canales, expedientes y facturación". No hay ninguna tabla de
facturación en el diagrama. O falta, o está fuera del alcance de esta migración.
Si va a existir, conviene saberlo antes de fijar `legal_entities`: arrastra series
de numeración fiscal por sociedad y por país, y esa numeración tiene requisitos
legales de continuidad que condicionan el modelo.

**9.8. Semántica de `persons.merged_into_id` (bloquea fase 2).** Al fusionar la
persona A en la B, ¿se reescriben las claves foráneas de A hacia B y queda una
lápida, o A conserva sus filas y toda lectura tiene que seguir la cadena? La
segunda opción mete un recursivo en el camino caliente de la lectura clínica. La
primera es irreversible. Hay que elegir antes de crear la columna.

**9.9. Asignaciones de denegación (bloquea fase 3).** Hoy la única forma de decir
"como el rol ADMIN pero sin ver X" es clonar la definición de rol. Azure lo
resuelve con asignaciones de tipo DENY evaluadas después de las de concesión. Sin
ellas, cada excepción es un rol nuevo y el catálogo se multiplica.

**9.10. `is_platform_admin` (bloquea fase 3).** Es un booleano que salta el modelo
de permisos entero: quien lo tiene lee la historia clínica de cualquier paciente
de cualquier tenant, sin caducidad, sin motivo y sin ámbito. Es exactamente por lo
que pregunta un auditor. La alternativa es que ese acceso pase por una asignación
temporal, motivada y auditada, del mismo modo que el acceso de un doctor externo.

**9.11. Cifrado del contenido de `crm_messages` (bloquea fase 6).** Los tokens de
canal se cifran; el cuerpo de los mensajes no. Por una conversación de WhatsApp
con un paciente viaja dato de salud.

**9.12. Particionado de `audit_logs` y `crm_messages` (bloquea fases 3 y 6).**
Auditar cada lectura de historia clínica, como exige el encargo, convierte
`audit_logs` en la tabla que más crece del sistema. Convertirla a particionada
después exige parada; decidirlo ahora no cuesta nada.

**9.13. RLS frente a `@Filter` de Hibernate (bloquea fase 3).** El encargo admite
las dos, pero no son equivalentes: `@Filter` no se aplica ni a `findById` ni a la
carga perezosa de un `@ManyToOne`, así que `repository.findById(idDeOtroTenant)`
sigue devolviendo la fila. Si la regla es que ninguna consulta pueda depender de
que el desarrollador recuerde filtrar, la única de las dos que la cumple es RLS.
Recomiendo RLS como mecanismo principal y `@Filter` como refuerzo.
