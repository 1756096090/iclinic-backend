# Plan de migración: nueva arquitectura de datos + Keycloak

Estado: borrador para aprobación. No se ha implementado nada.

Versión corregida del plan de cinco bloques, contrastada contra el repositorio el
2026-09-19. Sustituye al plan de ocho fases anterior. Las correcciones respecto a lo
que se propuso están marcadas con **[C-n]** y justificadas en §9.

```
                 A. Keycloak (identidad)  ─────────────┐
                                                        ├──►  D. Autorización fina
   B. Tenant: FK compuestas + RLS  ──►  C. patients/practitioners  ──┘
                                                        └──►  E. Agenda: EXCLUDE
```

A y B son independientes entre sí. C necesita B. D necesita A, B y C. E necesita C
y el `timestamptz` que arrastra.

---

## 0. Estado real del repositorio

Verificado, no supuesto. Varios puntos del contexto original ya no se cumplen.

| Comprobación | Contexto original | Realidad verificada |
|---|---|---|
| Flyway | "si no está, añádelo" | **Está.** `build.gradle:37-38`; `V1__baseline_schema.sql` cubre **19 tablas** |
| `spring.profiles.active` | — | `postgres` |
| `ddl-auto` | "validate en TODOS los perfiles" | `validate` en raíz y `postgres`; **`create-drop` en `h2`** |
| Firebase | 10 ficheros | 10 en `src/main/java` + `build.gradle`; **15** contando `src/` entero |
| `@PreAuthorize` | "solo UN fichero lo usa" | **Cero.** El único match es un javadoc en `Permission.java:26` |
| `LocalDateTime` | — | **25 ficheros** |
| Testcontainers | — | **0** |
| Trabajo sin publicar | "publícalo antes de empezar" | `main` va 1 commit por delante de `origin/main` (`4e02b45`) más 2 ficheros de `docs/` |

El commit sin publicar contiene Flyway, el baseline, la reorganización de perfiles,
`dev-seed.sql` y los documentos de arquitectura. **Publicarlo es el paso cero de
todo lo demás.**

### Numeración de migraciones

`V1` está aplicada. Las nuevas empiezan en `V2`.

**Flyway es lineal y el plan permite A y B en paralelo.** Si dos personas trabajan
a la vez, las dos numerarán a partir del último visible y colisionarán al
fusionar — o peor, no colisionarán en Git y Flyway fallará al arrancar con
«found more than one migration with version 4». Se reservan rangos por bloque:

| Bloque | Rango | Migraciones previstas |
|---|---|---|
| A | V2–V9 | `V2__users_keycloak.sql`, `V3__drop_firebase_columns.sql` |
| B | V10–V29 | `V10__claves_candidatas.sql`, `V11__denormalizar_company_id.sql`, `V12__fk_compuestas.sql`, `V13__rls.sql` |
| C | V30–V49 | `V30__patients_practitioners.sql`, `V31__backfill_clinico.sql`, `V32__appointments_repuntar.sql`, `V33__drop_columnas_viejas.sql` |
| D | V50–V69 | roles, vigencias, `patient_care_team`, `access_grants`, auditoría |
| E | V70–V89 | `timestamptz`, tabla `timezones`, rangos y `EXCLUDE` |

Los huecos se quedan vacíos: Flyway no exige numeración contigua. El coste de un
hueco es cero; el de una colisión es un arranque roto en el peor momento.

`V13__rls.sql` es la única con orden absoluto: se ejecuta **después** de toda
migración que cree tablas con `company_id`. Como C, D y E crean tablas nuevas, cada
una añade sus propias políticas al final de su rango en lugar de reabrir V13.

---

## Bloque A · Keycloak sustituye a Firebase

Autoservicio. No depende de ningún cambio de modelo.

### A.1 Infraestructura

Keycloak en `docker-compose.yml` con base propia, no la de la aplicación. Realm
`iclinic`, clientes `iclinic-api` (bearer-only) e `iclinic-web` (público, PKCE).
Realm exportado a `infra/keycloak/realm-iclinic.json` y versionado: un realm
configurado a mano en una consola no es reproducible.

### A.2 Client roles

Los nombres del enum `UserRole` que ya existe, para que la migración sea mecánica:

```
SUPER_ADMIN, ADMIN, DENTIST, ASSISTANT, RECEPTIONIST, EXTERNAL_DOCTOR, PATIENT
```

más dos nuevos: `BRANCH_MANAGER` y `BILLING`.

Es `DENTIST`, no `DOCTOR`. Es `ADMIN`, no `ORG_ADMIN`.

### A.3 Backend

- Fuera `com.google.firebase:firebase-admin`; dentro
  `spring-boot-starter-oauth2-resource-server`.
- `oauth2ResourceServer().jwt(...)` con un `JwtAuthenticationConverter` que lea
  `resource_access["iclinic-api"].roles` y las publique como `ROLE_*`.
- **Validación de audiencia.** Sin ella se acepta un token emitido para otro
  cliente: un `JwtClaimValidator` sobre `aud` añadido al
  `DelegatingOAuth2TokenValidator` junto al del issuer.
- `.anyRequest().denyAll()` en lugar de `.authenticated()`, con cada ruta
  declarada. Hoy un controlador nuevo nace abierto.

### A.4 Ficheros a tocar **[C-9]**

El criterio de aceptación es que `grep -ri firebase src/ build.gradle` no devuelva
nada. Eso son **15 ficheros**, no 10. La lista original omitía cinco:

| Fichero | Acción |
|---|---|
| `config/FirebaseConfig.java` | eliminar |
| `modules/auth/filter/FirebaseAuthenticationFilter.java` | eliminar |
| `modules/auth/controller/FirebaseAuthController.java` | eliminar |
| `modules/auth/service/FirebaseAuthSyncService.java` | eliminar |
| `config/SecurityConfig.java` | reescribir |
| `config/NotificationChannelInterceptor.java` | reescribir la validación del token STOMP |
| `modules/access/service/MembershipBackfillService.java` | quitar la referencia |
| `modules/user/dto/CreateUserRequestDto.java` | quitar la referencia |
| `shared/exception/GlobalExceptionHandler.java` | quitar el manejo de `FirebaseAuthException` |
| `build.gradle` | quitar la dependencia |
| `src/main/resources/application.properties` | quitar `firebase.service-account.path` |
| `src/main/resources/db/seed/dev-seed.sql` | los `external_auth_id` de ejemplo |
| `src/main/resources/import.sql` | ídem, perfil `h2` |
| `src/test/.../config/NotificationChannelInterceptorTest.java` | reescribir contra JWT de Keycloak |
| `src/test/.../shared/exception/FirebaseAuthExceptionHandlerTest.java` | **reescribir, no borrar** |

Ese último test cubre el manejo de excepciones de autenticación. El comportamiento
que verifica sigue siendo necesario con Keycloak, solo cambia la excepción de
origen. Se renombra a `AuthExceptionHandlerTest` y se adapta. No se elimina: la
regla de trabajo dice que un test no se borra para que pase el build.

### A.5 Tabla `users`

Migración `V2__users_keycloak.sql`:

```
keycloak_user_id    uuid UNIQUE
subject_type        varchar CHECK IN ('HUMAN','SERVICE_ACCOUNT')
keycloak_client_id  varchar
CHECK ((subject_type='SERVICE_ACCOUNT') = (keycloak_client_id IS NOT NULL))
tokens_valid_from   timestamptz NOT NULL DEFAULT now()
```

`V3__drop_firebase_columns.sql` retira `external_auth_id` y `auth_provider`.

`tokens_valid_from` es el interruptor de emergencia: en el filtro, tras validar la
firma, si `jwt.getIssuedAt() < user.tokensValidFrom` → 401. Un `UPDATE` corta todas
las sesiones de un usuario en la siguiente petición, sin estado de sesión en el
backend.

**[C-8] `users.role` también se retira aquí.** Hoy hay dos fuentes de rol:
`users.role` (que es la que lee `FirebaseAuthenticationFilter`) y
`company_memberships.role`. Si solo se sustituye la segunda, en el bloque D, queda
una columna huérfana que sigue pareciendo autoritativa. El filtro nuevo no la lee:
toma el rol del JWT y lo interseca con la membresía. La columna se marca obsoleta
en A y se elimina en D, cuando exista `company_membership_roles` que la sustituya.

### A.6 Migración de usuarios

Script idempotente y por lotes que cree los usuarios existentes en Keycloak vía
Admin API y rellene `keycloak_user_id`.

Los usuarios actuales no tienen contraseña en nuestro lado: hay que enviarles el
flujo de establecer contraseña de Keycloak. **Es un cambio visible para ellos.** Se
documenta en el PR y no se ejecuta sin avisar.

### A.7 Tokens

Access token 5 min. Refresh 8 h de inactividad, 12 h absoluto, para personal.
Offline tokens deshabilitados. MFA obligatorio (OTP condicional) para
`SUPER_ADMIN`, `ADMIN` y `DENTIST`.

### A.8 Criterios de aceptación

- `grep -ri firebase src/ build.gradle` no devuelve nada.
- Testcontainers con Keycloak: token con `DENTIST` entra en un endpoint de
  `DENTIST`; sin ese rol → 403; con `aud` distinto de `iclinic-api` → 401; emitido
  antes de `tokens_valid_from` → 401.
- Documentado qué cambia el frontend (OIDC + PKCE).

---

## Bloque B · Aislamiento de tenant: FK compuestas + RLS

### B.1 Claves candidatas

`UNIQUE (company_id, id)` en `branches`, `company_memberships`, `crm_contacts`,
`crm_channel_connections`, `crm_conversations`. **[C-7]** Los nombres reales de las
tres últimas llevan el prefijo `crm_`; el plan original decía `conversations`,
`channel_connections` y `channel_user_links`.

Una FK de N columnas necesita un `UNIQUE` con exactamente esas N columnas en ese
orden. Un `UNIQUE` de 2 no respalda una FK de 3. Donde haga falta una de 3
—`(company_id, branch_id, id)` en `branches`— se añade también ese `UNIQUE`.

**[C-1] `users` queda fuera de esta lista.** `users.company_id` es **NULLABLE**
(`V1__baseline_schema.sql:41`): los administradores de plataforma no tienen
empresa. Con nulos, `UNIQUE (company_id, id)` no impide duplicados, y la FK
`(company_id, doctor_id) → users` no puede resolverse porque
`appointments.company_id` sí es `NOT NULL`. La consecuencia práctica es que **la FK
de `appointments.doctor_id` no se crea en este bloque**: se crea en el bloque C
contra `practitioners`, cuyo `company_id` sí es `NOT NULL`. Hasta entonces, esa
relación sigue validándose en el servicio.

Alternativa, si se prefiere cerrarla ya: hacer `users.company_id` `NOT NULL` y
modelar los administradores de plataforma fuera de `users` o con una empresa
técnica. Es una decisión de modelo, no de migración. Ver §10.1.

### B.2 FK compuestas

Denormalizar `company_id` donde falte, rellenarlo desde el padre, ponerlo
`NOT NULL` y añadir la FK:

| Tabla | FK a crear |
|---|---|
| `membership_branches` | `+ company_id`; `(company_id, membership_id) → company_memberships`; `(company_id, branch_id) → branches` |
| `appointments` | `(company_id, branch_id) → branches`; `(company_id, contact_id) → crm_contacts` |
| `branch_schedules` | `+ company_id`; FK compuesta a `branches` |
| `branch_blocked_slots` | `+ company_id`; FK compuesta a `branches` |
| `crm_conversations` | `+ company_id`; FK compuestas a `crm_contacts` y `crm_channel_connections` |
| `crm_messages` | `+ company_id`; FK compuesta a `crm_conversations` |
| `crm_channel_user_links` | `+ company_id`; FK compuesta a `crm_contacts` |

La FK de `membership_branches` sustituye al comentario del javadoc de
`CompanyMembership`: «La integridad (branch.company == company) se valida en el
servicio». Tras esta migración, esa validación sobra y se retira.

Antes de cada FK, una consulta en la migración que detecte filas ya inconsistentes.
Si las hay, la migración **falla y las lista**. No se arreglan solas: son datos
reales.

Migraciones: `V10__claves_candidatas.sql`, `V11__denormalizar_company_id.sql`,
`V12__fk_compuestas.sql`.

### B.3 RLS

Dos roles de base de datos, y la distinción es lo que hace que RLS no sea
decorativa:

- `iclinic_app`: el de la aplicación. No propietario, sin `BYPASSRLS`, sin `DELETE`.
- `iclinic_migrator`: el de Flyway. Con `BYPASSRLS`.

Con `FORCE ROW LEVEL SECURITY` el propietario de la tabla también queda sujeto a la
política si no es superusuario. El DDL pasa, pero un backfill de migración afecta a
0 filas **sin dar error**: `UPDATE t SET x=...` → `UPDATE 0`, sin excepción. De ahí
`BYPASSRLS` para el migrador, y `SET row_security = off;` al principio de toda
migración que toque datos, que convierte ese silencio en
`ERROR: query would be affected by row-level security policy`. Un superusuario
siempre evita RLS: el problema no aparece probando como `postgres` y sí aparece en
producción.

Política generada en bucle para todas las tablas con `company_id`, no a mano: una
tabla sin política tiene cero aislamiento y nadie se entera.

```
ALTER TABLE <t> ENABLE ROW LEVEL SECURITY;
ALTER TABLE <t> FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON <t>
  USING      (company_id IS NOT DISTINCT FROM
              nullif(current_setting('iclinic.company_id', true), '')::bigint)
  WITH CHECK (company_id IS NOT DISTINCT FROM
              nullif(current_setting('iclinic.company_id', true), '')::bigint);
```

El `nullif(current_setting(..., true), '')` es lo que la hace fallar cerrada: sin
variable fijada, la política es `NULL` y no devuelve ninguna fila.

Migración: `V13__rls.sql`, ejecutada después de todas las que crean tablas.

### B.4 Contexto de tenant

`set_config('iclinic.company_id', ?, true)` por transacción, en un
`TransactionSynchronization` o un aspecto. El tercer parámetro a `true` la hace
local: se revierte al commit o al rollback y no contamina la conexión del pool.

`TenantContext` request-scoped: `sub` del JWT → `users` → `company_memberships`
activa. Si no es miembro de la empresa pedida → 403. Sin caché más allá de unos
segundos: la caché es justo lo que impide que revocar una membresía tenga efecto
inmediato.

### B.5 Perfiles y Testcontainers **[C-3]**

El plan original pedía `ddl-auto=validate` en todos los perfiles. **Eso deja el
perfil `h2` sin arrancar**: las migraciones son SQL de PostgreSQL
(`GENERATED BY DEFAULT AS IDENTITY`, `tstzrange`, `EXCLUDE`, `inet`) y Flyway está
deshabilitado ahí, así que con `validate` nadie crea el esquema.

Corrección: `validate` en la raíz y en `postgres`; el perfil `h2` conserva
`create-drop` como excepción documentada, tal como está hoy. A partir del bloque E
ese perfil pierde utilidad de todos modos, porque H2 no soporta `tstzrange` ni
`EXCLUDE` y `ddl-auto` no podrá generar el esquema real. La alternativa limpia es
eliminarlo en el bloque E y quedarse solo con `postgres` y Testcontainers. Ver
§10.2.

Testcontainers: `spring-boot-testcontainers`, `testcontainers:postgresql` y
`:junit-jupiter`, con una `AbstractPostgresIT` (`@ServiceConnection`, contenedor
estático reutilizado).

### B.6 Criterios de aceptación

Como `iclinic_app`, contra PostgreSQL real:

- Sin variable fijada: `SELECT count(*) FROM appointments` → 0.
- Empresa 1 fijada: se ven las suyas y ninguna de la 2.
- `SELECT * FROM appointments WHERE id = <id de la empresa 2>` → 0 filas. Éste
  demuestra que el IDOR entre empresas queda neutralizado en el motor.
- `INSERT` con `company_id` ajeno → error de política.
- Una prueba negativa por cada FK compuesta nueva.
- `iclinic_app` no tiene `BYPASSRLS` y no es propietario.
- Dos transacciones seguidas con empresas distintas sobre la misma conexión del
  pool no se filtran datos.
- Consulta de cobertura en CI que falle si hay una tabla con `company_id` sin
  política.

Rendimiento: con RLS, `company_id` debe ser la primera columna de los índices
compuestos. La política añade ese predicado a cada consulta; si no encabeza el
índice, se filtra después de leer. Se revisan los índices en este mismo PR.

---

## Bloque C · Modelo clínico: `patients` y `practitioners`

El más invasivo. Hoy el modelo confunde tres cosas: el paciente de una cita es un
`CrmContact`, el profesional es un `User` directo, y `User` hace de credencial, de
humano, de profesional y a veces de paciente. Eso impide tener número de historia,
licencia, especialidades y relación médico-paciente, y es la causa de que la
autorización no pueda responder «¿puede este doctor ver a *este* paciente?».

### C.1 Tablas nuevas

```
patients (
  id, company_id, medical_record_number, primary_branch_id, status, ...
  UNIQUE (company_id, id),
  UNIQUE (company_id, medical_record_number)
)
practitioners (
  id, company_id, user_id, license_number, license_country, license_status, status,
  UNIQUE (company_id, id),
  UNIQUE (company_id, user_id)
)
practitioner_branches (company_id, practitioner_id, branch_id)  PK compuesta
specialties (code PK)
practitioner_specialties (company_id, practitioner_id, specialty_code)  PK compuesta
```

**[C-5] `patients` no lleva `person_ref`.** El plan original pedía
`UNIQUE (company_id, person_ref)` y a la vez decía que no se cree la tabla
`persons`, así que `person_ref` no apuntaba a nada. Y proponía además
`crm_contacts.patient_id`, que es un segundo enlace para la misma relación.

Corrección: el enlace es **uno solo y va en `crm_contacts`**:

```
crm_contacts.patient_id   nullable
  FK compuesta (company_id, patient_id) → patients
  UNIQUE (company_id, patient_id)        -- un contacto como mucho por paciente
```

Razones: un paciente que llega sin pasar por el CRM (se presenta en mostrador) no
tiene contacto, y con `person_ref NOT NULL` en `patients` habría que inventarle uno
falso. La identidad del paciente es su propia fila más su
`medical_record_number`; el contacto de CRM es un canal por el que se le localiza,
no su identidad. Cuando exista `persons`, `patients` ganará `person_id` y ése será
el ancla; hasta entonces no hay ninguno, y está bien que no lo haya.

`UNIQUE (company_id, user_id)` en `practitioners` **no es cosmético**: es lo que
hace que un `EXCLUDE` sobre `practitioner_id` detecte al mismo humano agendado dos
veces a la misma hora aunque sea en sucursales distintas. Si se permiten dos filas
por humano, ese `EXCLUDE` deja de ver el choque y vuelve la doble reserva. No se
relaja.

### C.2 Migración de datos

Aquí está el riesgo: son datos reales de pacientes.

- `practitioners`: una fila por cada `User` con rol `DENTIST` o `EXTERNAL_DOCTOR`.
- `patients`: una fila por cada `CrmContact` con al menos una cita. Los contactos
  sin citas se quedan como contactos: un lead no es un paciente.
- `appointments` gana `patient_id` y `practitioner_id`, se rellenan, y **después**
  se retiran `contact_id` y `doctor_id`. En migraciones separadas, dejando las
  columnas viejas un release por si hay que volver atrás.
- Si un `CrmContact` tuviera citas en dos empresas, necesita dos `patients`. Se
  comprueba **antes** de migrar; si ocurre, se para y se avisa.
- Consulta de conteo antes y después dentro de la propia migración: ninguna cita
  puede perder su paciente ni su profesional.

Migraciones: `V30__patients_practitioners.sql`, `V31__backfill_clinico.sql`,
`V32__appointments_repuntar.sql`, y `V33__drop_columnas_viejas.sql` un release
después.

Aquí se crea también la FK que el bloque B tuvo que dejar pendiente
(**[C-1]**): `(company_id, practitioner_id) → practitioners`.

### C.3 Lo que no se hace todavía

No se separa `User` en `users` + `persons`. Es la pieza más invasiva y no hace
falta para la autorización: `practitioners.user_id` basta. Queda para un bloque
posterior.

### C.4 Criterios de aceptación

- Toda cita conserva paciente y profesional (conteo antes/después en la migración).
- Dos `practitioners` para el mismo `user` en la misma empresa → rechazado.
- `practitioner_branches` con una sucursal de otra empresa → rechazado.
- Ningún `CrmContact` con citas se queda sin `patient`.

---

## Bloque D · Autorización fina

Keycloak dice **qué rol** tienes. PostgreSQL dice **sobre qué** puedes ejercerlo.
Nunca se meten en Keycloak roles como `PATIENT_482_READ`: son cientos de miles, el
JWT explota, y una interconsulta que empieza ahora no puede esperar a que se
renueve el token.

### D.1 Varios roles por membresía

```
company_membership_roles (company_id, membership_id, role_code)  PK compuesta
CHECK role_code IN ('SUPER_ADMIN','ADMIN','BRANCH_MANAGER','DENTIST',
                    'ASSISTANT','RECEPTIONIST','EXTERNAL_DOCTOR','BILLING','PATIENT')
```

En una clínica pequeña la misma persona es recepcionista y asistente; con un rol
único hay que elegir el más permisivo.

El rol efectivo es la **intersección** del rol del JWT y el de esta tabla: quitar un
rol en PostgreSQL tiene efecto inmediato aunque el token vivo aún lo declare.
Keycloak es el catálogo; PostgreSQL es el interruptor.

**[C-8]** Aquí se elimina `company_memberships.role` y también `users.role`, que en
el bloque A quedó marcada como obsoleta.

### D.2 Vigencia en el acceso por sucursal

A `membership_branches` y `practitioner_branches`:

```
valid_from timestamptz NOT NULL DEFAULT now()
valid_until timestamptz              -- NULL = permanente
revoked_at, revoked_by_user_id, revocation_reason
CHECK (valid_until IS NULL OR valid_until > valid_from)
CHECK (revoked_at  IS NULL OR revoked_at >= valid_from)
CHECK ((revoked_at IS NULL) = (revoked_by_user_id IS NULL))
```

Resuelve «permanente en Cumbayá, hasta el 30/09 en Quito Norte».

Se mantiene la PK/UNIQUE de la tripleta **sin hacerla parcial**: un índice único
parcial no puede respaldar una FK y se perdería la garantía del bloque B.
Consecuencia asumida: una fila por par; reconceder es un `UPDATE` y el histórico
vive en la auditoría.

### D.3 `patient_care_team`

La relación clínica declarada cuando aún no hay cita.

```
(company_id, patient_id, practitioner_id, care_role)  PK compuesta
care_role CHECK IN ('PRIMARY','TREATING','ASSISTING')
+ las mismas columnas de vigencia
UNIQUE parcial (company_id, patient_id) WHERE care_role='PRIMARY' AND revoked_at IS NULL
CHECK (care_role <> 'PRIMARY' OR valid_until IS NULL)
```

Sin ese último `CHECK` el índice miente: un `PRIMARY` caducado por `valid_until`
seguiría bloqueando al sustituto, porque un índice no puede evaluar `now()`. Al
prohibir caducidad en `PRIMARY`, «no revocado» y «vigente» son lo mismo y el índice
es exacto. Al médico de cabecera se le sustituye **revocando**.

### D.4 `access_grants`

Sustituye a `ExternalDoctorPatientAccess`, que hoy tiene `patientId`, `companyId` y
`branchId` como `Long` sueltos sin FK, un booleano `active`, ninguna revocación y
nada que impida grants duplicados.

```
id, company_id,
grantee_membership_id  FK compuesta → company_memberships
patient_id  NULL       FK compuesta → patients
branch_id   NULL       FK compuesta → branches
scope_kind  GENERATED (PATIENT | BRANCH | ORGANIZATION)
permission, reason_code, justification, granted_by_user_id,
valid_from, valid_until,
validity tstzrange GENERATED ALWAYS AS (tstzrange(valid_from, valid_until,'[)')) STORED,
revoked_at, revoked_by_user_id, revocation_reason
```

Columnas tipadas con FK reales, no `resource_type`/`resource_id` como cadenas: con
texto la base de datos no puede impedir un grant sobre un paciente de otra empresa;
con FK compuesta, sí.

```
CREATE EXTENSION IF NOT EXISTS btree_gist;
CHECK (NOT (patient_id IS NOT NULL AND branch_id IS NOT NULL))
CHECK (reason_code IN ('CONSULTATION','SECOND_OPINION','COVERAGE','AUDIT',
                       'EMERGENCY','SUPPORT','ADMIN_TEMP'))
CHECK (valid_until IS NOT NULL OR reason_code = 'COVERAGE')
CHECK (scope_kind <> 'ORGANIZATION' OR reason_code IN ('AUDIT','SUPPORT'))
CHECK (length(btrim(justification)) >= 10)
CHECK (valid_until IS NULL OR valid_until - valid_from <= interval '2161 hours')
CHECK (reason_code <> 'EMERGENCY'
       OR (valid_until IS NOT NULL AND valid_until - valid_from <= interval '25 hours'))
EXCLUDE USING gist (company_id WITH =, grantee_membership_id WITH =,
                    permission WITH =, scope_kind WITH =,
                    COALESCE(patient_id, branch_id, 0) WITH =, validity WITH &&)
  WHERE (revoked_at IS NULL)
```

Los topes van en horas y por resta, no en días ni con suma. `valid_from + interval`
es `STABLE` y no vale en un `CHECK`; y `interval '90 days'` compara como 2160 h
exactas, mientras que 90 días de calendario que cruzan un cambio de hora son 2161.

Migración de cada `ExternalDoctorPatientAccess` a un grant con
`reason_code='CONSULTATION'`. Los que tengan `expiresAt` nulo reciben 30 días desde
`created_at` y **se listan en la salida de la migración** para que alguien los
revise.

### D.5 Auditoría **[C-6]**

El plan original exigía registrar el ALLOW y el DENY, pero ningún bloque creaba las
columnas. `audit_logs` hoy tiene `action`, `entity_type`, `entity_id`,
`metadata varchar(2000)` y `created_at`, y nada más.

Corrección: este bloque incluye la migración mínima que su propio criterio necesita.

```
occurred_at     timestamptz NOT NULL DEFAULT now()   -- renombra created_at
actor_subject   uuid            -- el `sub` de Keycloak; vale para humano y servicio
actor_type      varchar CHECK IN ('HUMAN','SERVICE_ACCOUNT','SYSTEM')
CHECK (actor_type = 'SYSTEM' OR actor_subject IS NOT NULL)
outcome         varchar CHECK IN ('ALLOW','DENY','ERROR')
deny_reason     varchar
grant_path      varchar         -- la VÍA por la que se concedió
request_id      uuid
ip_address      inet
metadata        jsonb           -- desde varchar(2000)
```

`iclinic_app` recibe `INSERT` y nada más sobre esta tabla: una auditoría que la
aplicación puede modificar no prueba nada.

Lo que **no** entra aquí y queda para un bloque F: particionar por mes, la política
de retención y la deduplicación de lecturas de alta frecuencia. Son trabajo de
operación, no de autorización, y particionar después exige parada — conviene
decidirlo pronto aunque se ejecute más tarde. Ver §10.4.

### D.6 `AuthorizationService`

Bean `authz` en `shared/authorization`. Métodos: `canReadPatient`,
`canReadAppointment`, `canCreateAppointment`, `canUpdateAppointment`,
`canManageBranch`, `canReadConversation`, `canSendCrmMessage`.

Cada uno, siempre en este orden:

1. Tenant: el objeto se resuelve dentro de la empresa actual, o no existe.
2. Estado del actor: usuario y membresía activos.
3. Permiso general: rol del JWT ∩ `company_membership_roles`.
4. Relación con el objeto concreto.
5. Auditoría: el ALLOW y también el DENY, con su motivo.

Devuelve **la vía** por la que se concedió, no un booleano: en una inspección hay
que poder decir si fue por una cita, por el equipo tratante o por un grant.

### D.7 «¿Puede leer a este paciente?»

Seis vías, la más normal primero:

- **(a)** `ADMIN` / `SUPER_ADMIN` de la empresa.
- **(b)** `BRANCH_MANAGER`, contra **sus** sucursales vigentes. Rama separada de (a)
  y con etiqueta propia: metido en (a) le daría acceso a toda la empresa. El
  paciente pertenece a la sucursal por `primary_branch_id` **o por tener actividad
  allí**; `primary_branch_id` es nullable y apoyarse solo en él deniega en silencio
  a todo paciente registrado sin sucursal asignada.
- **(c)** `patient_care_team` vigente.
- **(d)** Continuidad asistencial, **dos reglas distintas**: cita **atendida** →
  acceso permanente; cita **agendada** → ventana `[inicio − 24 h, fin + 24 h)`. No
  vale solo `now() >= inicio - 24h`: es cierto para cualquier cita pasada, así que
  una cita olvidada de hace tres años daría acceso permanente y el estado de
  atendida no serviría para nada.
- **(e)** `access_grants` sobre ese paciente, con `validity @> now()` y
  `revoked_at IS NULL`.
- **(f)** `access_grants` de ámbito sucursal o empresa.

Los plazos de (d) van en configuración, no como constantes en el código.

### D.8 Service accounts

Las integraciones (agente de WhatsApp, scheduler, batch) usan Service Accounts de
Keycloak, no usuarios humanos falsos, y pasan por el mismo `AuthorizationService`
sin bypass. Su alcance de empresa va en:

```
service_account_grants (company_id, user_id, scopes text[])
CHECK (scopes <@ ARRAY['appointment.read','appointment.create','appointment.write',
                       'patient.basic.read','crm.read','crm.send','file.read'])
```

`clinical.*` no está en la lista a propósito: la historia clínica no se lee con un
token de máquina. Se prohíbe también en código por `subject_type`, para que una
casilla mal marcada en la consola de Keycloak no pueda habilitarlo.

Sin esta tabla, un service account autenticado opera sobre cualquier empresa,
porque su token no dice de quién es.

### D.9 Cerrar el IDOR

- `@PreAuthorize("@authz.canReadAppointment(#id)")` en todo endpoint con id en la
  ruta. **[C-2] Hoy no lo usa ningún endpoint**: el único match de `@PreAuthorize`
  en el proyecto es un comentario javadoc en `Permission.java:26`. No hay ningún
  ejemplo previo del que partir, así que el PR debe incluir el primero completo,
  con su test, como plantilla del resto.
- Repositorios: prohibido buscar sin tenant en el código de negocio.
  `findByBranchIdOrderByScheduledStartAsc(branchId)` →
  `findByCompanyIdAndBranchIdOrderByScheduledStartAsc(companyId, branchId)`.
- Códigos, y es deliberado: objeto de **otra** empresa → 404 (un 403 confirmaría
  que existe en otra clínica); objeto de **mi** empresa que no me corresponde → 403
  (así se pide una interconsulta en lugar de compartir credenciales).
- Prueba ArchUnit que falle la compilación si un método de `@RestController` tiene
  un `@PathVariable` acabado en `Id` sin `@PreAuthorize` ni marca de público. Las
  revisiones se olvidan; la compilación no. **ArchUnit es una dependencia nueva que
  no estaba en ninguna lista aprobada**: ver §10.3.
- Trampa más frecuente: autorizar un registro clínico por su id comprobando solo el
  tenant. Dentro de una misma clínica eso deja a cualquier doctor leer a cualquier
  paciente cambiando el número. Regla única: se resuelve el **paciente** del
  registro y se le aplica la regla del paciente. Nunca hay una regla propia del
  registro.

### D.10 Criterio de aceptación de punta a punta

Dra. Ana, rol `DENTIST`, sucursal habitual Cumbayá. Paciente 845 se atiende en
Quito Norte. Recibe una interconsulta de 48 h con permiso `clinical.read`.

| Situación | Esperado |
|---|---|
| Sin grant | 403 |
| Durante la ventana, paciente 845 | 200 |
| Durante la ventana, paciente 846 | 403 |
| Pasadas las 48 h, sin borrar nada | 403 |
| Revocado en mitad de la ventana | 403 inmediato |
| Grant duplicado activo solapado | rechazado por la base de datos |
| Grant `EMERGENCY` de 7 días | rechazado por la base de datos |
| Grant de `clinical.sign` | rechazado por la base de datos |

---

## Bloque E · Agenda: `tstzrange` y `EXCLUDE`

Hoy el anti-solapamiento es
`findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan`: un
leer-y-luego-escribir. Dos peticiones concurrentes ven el hueco libre y las dos
insertan.

### E.1 Primero `timestamptz`

Un `tstzrange` construido sobre columnas `timestamp` sin zona está mal desde el
origen.

- `LocalDateTime` → `Instant` en `Appointment.scheduledStart`/`scheduledEnd`,
  `BranchBlockedSlot` y las fechas de vigencia de `BranchSchedule`. **No** se tocan
  `BranchSchedule.startTime`/`endTime`: son `LocalTime`, hora de pared, y está bien.
- `ALTER ... TYPE timestamptz USING <col> AT TIME ZONE '<zona de captura>'`.
  **Bloqueado por §10.5:** si hay datos de Colombia mezclados con Ecuador, convertir
  con una sola zona desplaza citas históricas y no es reversible.
- `branches.timezone` (IANA) `NOT NULL`. **No** validado con regex: el patrón
  `'^[A-Za-z]+/...'` rechaza `UTC`, que es válida, y acepta `Marte/Olympus`, que no.
  Se usa una tabla `timezones` poblada desde `pg_timezone_names` y una FK.
- Todo `now()` de `AppointmentServiceImpl` que decida disponibilidad se resuelve en
  la zona de la **sucursal**, no en la del servidor.

### E.2 Rangos y `EXCLUDE`

```
CREATE EXTENSION IF NOT EXISTS btree_gist;
slot            tstzrange NOT NULL
scheduled_start timestamptz GENERATED ALWAYS AS (lower(slot)) STORED
CHECK (NOT isempty(slot) AND lower_inc(slot) AND NOT upper_inc(slot))
```

`lower(tstzrange)` es inmutable, así que la columna generada funciona y no puede
contradecir al rango.

**[C-4] Los tres `EXCLUDE` no van sobre el mismo rango.** El plan original ponía el
del profesional y el del paciente sobre `slot`, e introducía después
`slot_blocking` sin decir que ninguno se moviera; así el buffer no se respeta y
`slot_blocking` no sirve para nada. El reparto correcto:

| Constraint | Columna | Rango | Por qué |
|---|---|---|---|
| Profesional | `practitioner_id` | **`slot_blocking`** | El buffer es tiempo del doctor |
| Paciente | `patient_id` | `slot` | Al paciente no le afecta el buffer del doctor |
| Sillón | `resource_id` | `slot` | El sillón se libera al acabar la cita |

Los tres **parciales**, con `WHERE status NOT IN ('CANCELLED','NO_SHOW')`. Sin ese
`WHERE`, cancelar y reagendar en el mismo hueco sería imposible. El de sillón lleva
además `resource_id IS NOT NULL`.

Antes de crearlos, detectar solapamientos ya existentes. Si los hay, la migración
falla y los lista.

### E.3 Buffer

Si se añade buffer posterior al servicio: no se lee del tipo de servicio al
consultar. Se congela en la cita (`buffer_after_minutes`) y se deriva
`slot_blocking` por trigger.

- No puede ser columna generada: `timestamptz + interval` no es inmutable
  (`ERROR: generation expression is not immutable`).
- El trigger va `BEFORE INSERT OR UPDATE`, **sin lista de columnas**. Con
  `UPDATE OF slot, buffer_after_minutes`, un `UPDATE ... SET slot_blocking = ...` no
  lo dispara y se puede inflar el bloqueo de un profesional a voluntad.

Si cambia el buffer del servicio, el histórico no se reescribe: el buffer del
servicio es el valor por defecto al agendar, no la definición del pasado.

### E.4 Servicio

Se conserva la comprobación previa —da el mensaje de error bonito en el caso
normal— y se añade el manejo de la violación del `EXCLUDE`: capturar
`DataIntegrityViolationException`, distinguir por nombre de constraint y traducir a
409 con un mensaje por caso. La comprobación previa es para la experiencia de uso;
el constraint es la garantía.

### E.5 Criterios de aceptación

- Test de concurrencia real con Testcontainers: dos hilos crean la misma cita a la
  vez; exactamente uno gana y el otro recibe 409. **Este test debe fallar si se
  quita el `EXCLUDE`** — comprobarlo antes de darlo por bueno.
- Cancelar y reagendar en el mismo hueco → funciona.
- Mismo paciente con dos doctores a la misma hora → rechazado.
- Mismo profesional en dos sucursales a la misma hora → rechazado.
- Un instante sobrevive a cambiar `-Duser.timezone` de la JVM.

---

## 9. Resumen de las correcciones aplicadas

| Ref | Corrección | Bloque |
|---|---|---|
| C-1 | `users` sale de la lista de claves candidatas y de FK compuestas: `users.company_id` es nullable. La FK del profesional se crea en C contra `practitioners` | B, C |
| C-2 | `@PreAuthorize` no se usa en ningún endpoint; el único match es un javadoc. El PR debe incluir el primer ejemplo completo | D |
| C-3 | `ddl-auto=validate` en todos los perfiles deja `h2` sin arrancar. Se mantiene `create-drop` ahí como excepción documentada | B |
| C-4 | El `EXCLUDE` del profesional va sobre `slot_blocking`; paciente y sillón sobre `slot` | E |
| C-5 | `patients.person_ref` no estaba definido y duplicaba `crm_contacts.patient_id`. Se queda solo el segundo | C |
| C-6 | La auditoría de ALLOW/DENY que D exige necesita columnas que ningún bloque creaba. Se añade la migración mínima a D | D |
| C-7 | Nombres reales de tabla: `crm_conversations`, `crm_channel_connections`, `crm_channel_user_links` | B |
| C-8 | `users.role` sobrevivía a la migración de roles. Se marca obsoleta en A y se elimina en D | A, D |
| C-9 | El criterio de Firebase afecta a 15 ficheros, no 10. `FirebaseAuthExceptionHandlerTest` se reescribe, no se borra | A |

---

## 10. Decisiones pendientes

**10.1. `users.company_id` nullable.** Con la corrección C-1 el bloque B no se
bloquea, pero la pregunta sigue: ¿`users.company_id` pasa a `NOT NULL` con los
administradores de plataforma modelados aparte, o `users` se queda fuera del
esquema de FK compuestas de forma permanente? Afecta al bloque C.

**10.2. Futuro del perfil `h2`.** A partir del bloque E no puede reproducir el
esquema real. O se elimina y se depende de Testcontainers, o se acepta que diverge
y solo sirve para arrancar la aplicación.

**10.3. ArchUnit.** Dependencia nueva que el criterio de aceptación de D exige y
que no está en ninguna lista aprobada.

**10.4. Particionado y retención de `audit_logs`.** Auditar cada lectura clínica la
convierte en la tabla que más crece. Convertirla a particionada después exige
parada. Se puede diferir a un bloque F, pero la decisión conviene tomarla ahora.

**10.5. Zona horaria de los datos existentes.** Bloquea el bloque E. Si hay datos de
Ecuador y Colombia mezclados, una sola zona de conversión desplaza citas y no es
reversible.

**10.6. ¿Hay algún entorno con datos reales?** Bloquea la forma de los backfills de
B y C y la detección de solapamientos de E. Si solo existe `dev-seed.sql`, se pueden
asumir datos limpios y las migraciones se simplifican mucho.

**10.7. Publicar el trabajo local.** Un commit sin subir más dos ficheros
modificados, antes de abrir el primer PR.
