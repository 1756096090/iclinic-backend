# PLAN — Refactor CRM omnicanal multi-tenant

Estado: **Fase 0 completada** (build verde contra PostgreSQL real). Fases 1–6 pendientes.
El registro de avance está en §6.

---

## 0. Qué encontré en el repo

### 0.1 Lo que ya está alineado con el objetivo

| Pieza actual | Comentario |
|---|---|
| `crm_channel_connections` (`ChannelConnection`) | Ya es una fila por cuenta, con `company_id`, `branch_id`, `access_token_encrypted`, `webhook_verify_token`. La tabla ya soporta N cuentas; **lo que lo impide es el código que la consulta**, no el esquema. |
| `crm_channel_user_links` (`ChannelUserLink`) | Es, de hecho, `contact_identities` con otro nombre: `(contact_id, channel_type, external_user_id, external_chat_id)`. |
| `SecretEncryptionService` | AES-256/GCM ya implementado, clave por `ENCRYPTION_SECRET_KEY`. |
| `MessagingChannelAdapterRegistry` | Registry por inyección de `List<Adapter>`, auto-descubre. |
| `modules/access/` | `CompanyMembership`, `Role`, `Permission`, `AuditLog` ya existen (Fase A de `AD MULTITENANCY_SAAS_PLAN.md`), aún sin uso efectivo. |
| `CurrentUserService` | Tiene `assertCanAccessCompany/Branch`, `getCurrentCompanyId`, `assertBranchInCompany`. |

### 0.2 Los acoplamientos reales que hay que romper

1. **Una cuenta por (empresa, tipo de canal).** No es el esquema, son estas consultas, todas `Optional`:
   - `ChannelConnectionRepository.findActiveByCompanyAndChannel(companyId, channelType, statuses)` → usada en `ChannelInboundMessageProcessor`, `MessageServiceImpl.processInboundMessage` y `TelegramWebhookController.validateTelegramSecret`.
   - `findByCompanyIdAndChannelTypeAndStatus(...)`.

   Con dos WhatsApp en la misma empresa devuelven `NonUniqueResultException` o, peor, la cuenta equivocada.

2. **Fuga de tenant en las identidades de canal.** `ChannelUserLink` tiene `UNIQUE (channel_type, external_user_id)` **global**, y `CrmContactServiceImpl.resolveContact` resuelve con `findByChannelTypeAndExternalUserId(...)` **sin filtrar por empresa**. Si el mismo número de WhatsApp escribe a dos clínicas distintas, la segunda reusa el `CrmContact` de la primera → mezcla de datos entre tenants. Esto es un bug presente hoy, no sólo un obstáculo al refactor.

3. **Una conversación abierta por contacto, sin importar el canal.** `ConversationServiceImpl.findOrCreateOpenConversation` busca con `findFirstByContactIdAndStatusOrderByCreatedAtDesc(contactId, OPEN)` e **ignora el `channelConnection`**. Un contacto que escribe por Telegram y por WhatsApp acaba con los dos hilos en la misma conversación, y las respuestas salen por el canal de la conversación original.

4. **Webhooks acoplados al proveedor.**
   - `POST /api/v1/crm/webhooks/telegram/{companyId}` — el tenant viene de la URL; el secreto se valida contra "la" conexión Telegram de esa empresa.
   - `POST /api/v1/crm/webhooks/meta` — sin token de ruta y sin validación de firma `X-Hub-Signature-256`; `verifyWebhook` acepta cualquier `webhookVerifyToken` que exista en la tabla.
   - `MetaWebhookServiceImpl` parsea con `JsonPointer` sólo `entry[0].changes[0]...` e ignora el resto del lote.

5. **Idempotencia sólo en memoria.** `ChannelInboundMessageProcessor.checkDuplicate` hace un `SELECT` previo, pero **no hay índice único** sobre `(channel_type, external_message_id)`. Dos entregas concurrentes de Meta insertan dos filas.

6. **Adapter anémico.** `MessagingChannelAdapter` sólo tiene `supports()` y `sendText()`. Parseo, verificación de firma y capacidades viven dispersos en servicios específicos por proveedor.

7. **DDL generada por Hibernate.** `create-drop` en h2, `update` en postgres, `import.sql` como seed. No hay Flyway ni migraciones versionadas.

8. **Sin aislamiento transversal.** No existe `TenantContext` ni filtro Hibernate; cada servicio recuerda (o no) filtrar por `company_id`. `ConversationController`, `MessageController` y `ChannelConnectionController` exponen `getAll()` sin filtro de empresa.

9. **`SecretEncryptionService` con dos puertas traseras**: acepta el prefijo `{plain}` y, en `decrypt`, si el payload mide menos de 28 bytes lo devuelve como texto plano ("legacy base64"). Ambas se usan en `import.sql`.

---

## 1. Decisiones que se apartan de tu especificación

### 1.1 `contact_identities` debe ser único por empresa, no global

Pediste `(contact_id, channel_type, external_id UNIQUE)`. Ese `UNIQUE` sobre `external_id` global reproduce el bug 0.2.2: el mismo `+593987654321` no podría pertenecer a un contacto de la Clínica A y a otro de la Clínica B.

Propongo:

```
contact_identities(
  id, contact_id, company_id, channel_type, external_id, external_chat_id,
  username, display_name, created_at, updated_at)
UNIQUE (company_id, channel_type, external_id)
UNIQUE (contact_id, channel_type, external_id)
```

La unicidad es **por empresa, no por cuenta**: si una clínica tiene dos números de WhatsApp y el mismo paciente escribe a ambos, sigue siendo una sola persona. Es justo lo que permite unificar identidades, que era el objetivo de la tabla.

### 1.2 `SELECT ... FOR UPDATE SKIP LOCKED` no existe en H2

El worker de campañas con `SKIP LOCKED` sólo corre en PostgreSQL. Consecuencias:

- El perfil `test` con H2 **no puede** ejercitar la query real de claim.
- Opciones: (a) tests unitarios del worker con repositorio mockeado + un test de integración anotado `@EnabledIfSystemProperty` que sólo corre con Postgres levantado; (b) meter Testcontainers.
- **Mi recomendación: (a)**, para no añadir Docker como dependencia de `./gradlew test`. Dime si prefieres Testcontainers.

### 1.3 El `@Filter` de Hibernate no cubre todo

`@Filter` no se aplica a `EntityManager.find()` / `repository.findById()` ni a la carga lazy de un `@ManyToOne`. Es decir: `conversationRepository.findById(idDeOtraEmpresa)` **seguirá devolviendo la fila**. El filtro es defensa en profundidad para queries derivadas y JPQL; los `assert*` explícitos siguen siendo obligatorios en los puntos de entrada. Lo implemento igual, pero no quiero que quede la impresión de que el problema se cierra solo.

### 1.4 Meta necesita un campo nuevo: `app_secret`

Verificar `X-Hub-Signature-256` requiere el **App Secret** de la app de Meta, que hoy no se guarda en ninguna parte. Añado `app_secret_encrypted` a la cuenta de canal. Sin él, `verifySignature` para Meta no puede hacer nada real.

### 1.5 Campañas de WhatsApp: plantillas aprobadas, no texto libre

Fuera de la ventana de 24 h, Meta sólo acepta mensajes de plantilla (`type: template`, con `name` + `language` aprobados en el Business Manager). Por eso `message_templates` guarda `provider_template_name` + `language_code` + variables, no un cuerpo de texto arbitrario. Telegram y webchat sí aceptan texto libre; el mismo modelo lo cubre con `body` opcional.

### 1.6 Librerías nuevas que necesito

| Librería | Para qué | ¿Imprescindible? |
|---|---|---|
| `org.flywaydb:flyway-core` + `flyway-database-postgresql` | Migraciones versionadas | Sí, lo pediste |
| `com.h2database:h2` | Perfil `test` | Ya presente |
| `org.testcontainers:postgresql` | Sólo si eliges 1.2(b) | Opcional, espero tu respuesta |

No necesito nada más: el throttle, el backoff y el outbox se hacen con `@Scheduled` + JPA. **No voy a meter Quartz, Redis ni RabbitMQ** salvo que me lo pidas.

---

## 2. Modelo de datos objetivo

```
crm_channel_accounts            (antes crm_channel_connections)
  id, company_id, branch_id, channel_type, provider, display_name,
  external_account_id,            -- phone_number_id | page_id | bot username | ig user id
  business_account_id,            -- WABA id / business id
  access_token_encrypted, app_secret_encrypted,
  webhook_token UNIQUE,           -- segmento de la URL pública del webhook
  webhook_verify_token,           -- hub.verify_token de Meta
  webhook_registered_url,
  status, throttle_per_minute, config_json,
  created_at, updated_at, deleted_at
  UNIQUE (company_id, channel_type, external_account_id) WHERE deleted_at IS NULL
  UNIQUE (channel_type, external_account_id) WHERE deleted_at IS NULL   -- una cuenta externa = un tenant

contact_identities              (antes crm_channel_user_links)
  id, contact_id, company_id, channel_type, external_id, external_chat_id,
  username, display_name, created_at, updated_at
  UNIQUE (company_id, channel_type, external_id)

crm_conversations
  + channel_account_id          (reemplaza channel_connection_id)
  + contact_identity_id         (por qué identidad concreta llegó el hilo)
  UNIQUE (contact_id, channel_account_id) WHERE status = 'OPEN'

crm_messages
  + channel_account_id
  + provider_message_id         (renombra external_message_id)
  + failure_reason, sent_at, delivered_at, read_at
  UNIQUE (channel_account_id, provider_message_id) WHERE provider_message_id IS NOT NULL

webhook_events                  (nueva — auditoría e idempotencia de entrega)
  id, channel_account_id, provider, provider_event_id, raw_payload,
  signature_valid, processed_at, error, received_at
  UNIQUE (channel_account_id, provider_event_id)

modules                         (catálogo global)
  id, code UNIQUE, name, description, active
company_modules
  id, company_id, module_id, enabled, enabled_at, disabled_at, settings_json
  UNIQUE (company_id, module_id)

message_templates
  id, company_id, channel_type, name, provider_template_name, language_code,
  body, variables_json, status, created_at, updated_at
  UNIQUE (company_id, channel_type, name)

campaigns
  id, company_id, channel_account_id, template_id, name, status,
  scheduled_at, started_at, finished_at, rate_per_minute,
  total_recipients, sent_count, failed_count, created_by_user_id,
  created_at, updated_at

campaign_recipients             (el outbox)
  id, campaign_id, contact_id, contact_identity_id, company_id,
  status,                       -- PENDING|CLAIMED|SENT|DELIVERED|READ|FAILED|SKIPPED
  variables_json, provider_message_id, attempts, next_attempt_at,
  last_error, claimed_at, sent_at, updated_at
  UNIQUE (campaign_id, contact_identity_id)
  INDEX (status, next_attempt_at)   -- el índice que usa el worker
```

`ChannelType` pasa a `TELEGRAM, WHATSAPP, INSTAGRAM, FACEBOOK, WEBCHAT`.

---

## 3. Fases

Cada fase: compila, tests en verde, resumen de cambios + cURL de prueba. Paro entre fases.

---

### Fase 0 — PostgreSQL + Flyway, sin cambio funcional

**Objetivo:** congelar el esquema actual en una migración baseline y apagar `ddl-auto`.

Crear:

- `src/main/resources/db/migration/V1__baseline_schema.sql` — DDL completa del esquema actual (la genero volcando el `hbm2ddl` a fichero, luego la reviso a mano y le añado los índices que faltan).
- `src/main/resources/db/migration/V2__seed_dev_data.sql` — el contenido de `import.sql` traducido a Postgres, con `INSERT ... ON CONFLICT DO NOTHING`, aplicado sólo en perfiles `dev`/`postgres` vía `spring.flyway.locations`.
- `src/main/resources/application-test.properties` — H2 en modo `PostgreSQL`, `ddl-auto=create-drop`, Flyway apagado. Es el único sitio donde sobrevive `ddl-auto`.
- `src/test/resources/application-test.properties`.

Modificar:

- `build.gradle` — Flyway.
- `application.properties` — `spring.profiles.active=postgres`, `spring.jpa.hibernate.ddl-auto=validate`.
- `application-postgres.properties` — `ddl-auto=validate`, `spring.flyway.enabled=true`.
- `application-h2.properties` — se mantiene para arranque local sin Docker, con `ddl-auto=create-drop` + `import.sql`, **marcado explícitamente como perfil de juguete**. Si prefieres eliminarlo del todo, dímelo.
- `docker-compose.yml` — sin cambios; ya levanta postgres:16.

Rompe: arrancar con el perfil por defecto pasa a exigir Postgres levantado. Es el coste de "toda la estructura vive en migraciones versionadas".

---

### Fase 1 — `ChannelAccount` + `contact_identities` + conversación por cuenta

Migraciones:

- `V3__rename_channel_connections_to_accounts.sql`
  `ALTER TABLE crm_channel_connections RENAME TO crm_channel_accounts`; añade `display_name`, `business_account_id`, `app_secret_encrypted`, `webhook_token`, `throttle_per_minute`, `config_json`; backfill de `webhook_token` con `gen_random_uuid()`; índices únicos nuevos.
- `V4__rename_channel_user_links_to_contact_identities.sql`
  Renombra tabla y columnas; añade `company_id` **con backfill desde `crm_contacts.company_id`**; sustituye el `UNIQUE (channel_type, external_user_id)` por `UNIQUE (company_id, channel_type, external_id)`.
  Riesgo de datos: si hoy existen dos empresas compartiendo un `external_user_id`, el backfill los separa correctamente porque el `company_id` viene del contacto. Si existe un contacto ya "fusionado" por el bug 0.2.2, la migración lo deja como está y emite un `SELECT` de reporte; la limpieza de esos casos es manual y la documento.
- `V5__conversations_and_messages_channel_account.sql`
  `channel_connection_id` → `channel_account_id`; añade `contact_identity_id` (backfill por `contact_id` + `channel_type`); añade el índice único parcial de conversación abierta; renombra `external_message_id` → `provider_message_id`; añade `channel_account_id` a `crm_messages` (backfill vía conversación) y el **índice único de idempotencia**.

Crear:

- `crm/channel/entity/ChannelAccount.java` (extiende `BaseEntity`).
- `crm/channel/repository/ChannelAccountRepository.java` — métodos que devuelven **`List`**, no `Optional`: `findByCompanyIdAndChannelType`, `findByWebhookToken`, `findByChannelTypeAndExternalAccountId`.
- `crm/contact/entity/ContactIdentity.java`, `crm/contact/repository/ContactIdentityRepository.java`.
- `crm/channel/service/ChannelAccountService(+Impl).java`, `crm/channel/mapper/ChannelAccountMapper.java`.
- DTOs: `ChannelAccountResponseDto`, `CreateChannelAccountRequestDto`, `UpdateChannelAccountRequestDto`.
- `crm/exception/ChannelAccountNotFoundException.java`, `AmbiguousChannelAccountException.java`.

Modificar:

- `CrmContactServiceImpl.resolveContact` — resolver **siempre** por `(companyId, channelType, externalId)`.
- `ConversationServiceImpl.findOrCreateOpenConversation` / `hasOpenConversation` — clave `(contact, channelAccount)`.
- `ConversationRepository` — `findFirstByContactIdAndChannelAccountIdAndStatus...`.
- `MessageServiceImpl` — usa `ContactIdentity` en vez de `ChannelUserLink`; el envío resuelve la cuenta desde la conversación (ya lo hace) en lugar de re-buscarla por empresa.
- `ChannelInboundMessageProcessor` — recibe la `ChannelAccount` ya resuelta por el webhook; deja de buscarla.
- Eliminar `ChannelUserLink`, `ChannelUserLinkRepository`, `ChannelUserLinkService`.

**Cambio de contrato (CRM):**

| Antes | Después |
|---|---|
| `GET /api/v1/crm/channels` (todas, sin filtro de tenant) | `GET /api/v1/crm/channel-accounts` (filtrado por tenant) |
| `POST /api/v1/crm/channels` | `POST /api/v1/crm/channel-accounts` |
| `PATCH /api/v1/crm/channels/{id}/activate` | `PATCH /api/v1/crm/channel-accounts/{id}/activate` |
| `GET /api/v1/crm/channels/company/{companyId}` | `GET /api/v1/crm/channel-accounts?companyId=` |
| Respuesta: `{id, branchId, channelType, provider, externalAccountId, externalPhoneNumberId, status, createdAt}` | `{id, companyId, branchId, channelType, provider, displayName, externalAccountId, businessAccountId, webhookUrl, status, throttlePerMinute, createdAt}` |
| `ConversationResponseDto.channelConnectionId` | `channelAccountId` |
| `MessageResponseDto.externalMessageId` | `providerMessageId` |

Las rutas viejas `/api/v1/crm/channels/**` quedan como `@Deprecated` delegando en las nuevas durante esta fase, y se borran en la Fase 6. Appointments, users, branches y companies no se tocan.

Tests: `ChannelAccountServiceImplTest`, `ContactIdentityResolutionTest` (incluye el caso cross-tenant del bug 0.2.2), `ConversationPerAccountTest`, `MessageServiceImplTest` (actualizado).

---

### Fase 2 — Webhook genérico + Adapter con la forma nueva

Crear:

- `crm/channel/adapter/MessagingChannelAdapter.java` (reescrito):

  ```java
  ChannelType type();
  boolean verifySignature(ChannelAccount acc, String rawBody, Map<String,String> headers);
  List<InboundEvent> parse(ChannelAccount acc, String rawBody);
  SendResult send(ChannelAccount acc, OutboundMessage msg);
  Set<Capability> capabilities();
  ```

- `crm/channel/adapter/dto/`: `InboundEvent`, `OutboundMessage`, `SendResult`, `Capability` (enum: `TEXT, IMAGE, DOCUMENT, TEMPLATE, BUTTONS, LOCATION, DELIVERY_RECEIPTS, READ_RECEIPTS`).
- `crm/webhook/controller/ChannelWebhookController.java`:
  - `GET  /api/v1/crm/webhooks/{channelType}/{webhookToken}` — verificación (`hub.challenge` de Meta), compara `hub.verify_token` contra **esa** cuenta.
  - `POST /api/v1/crm/webhooks/{channelType}/{webhookToken}` — resuelve cuenta por token; en Meta, si el body trae un `entry[].changes[].value.metadata.phone_number_id` distinto, re-resuelve por ese id (una app de Meta sirve a varios números). Verifica firma → persiste `webhook_events` → parsea → procesa cada `InboundEvent`.
- `crm/webhook/service/WebhookDispatcher.java` — orquesta lo anterior; devuelve `200` siempre que la firma sea válida, incluso si el procesamiento de un evento individual falla (evita reintentos infinitos del proveedor); los fallos quedan en `webhook_events.error`.
- `crm/channel/adapter/meta/MetaSignatureVerifier.java` — HMAC-SHA256 con `app_secret`, comparación en tiempo constante.
- `crm/channel/adapter/webchat/WebchatAdapter.java` — canal propio, sin proveedor externo.
- Migración `V6__webhook_events.sql`.

Modificar:

- `TelegramAdapter`, `WhatsAppMetaAdapter` → implementan la interfaz nueva; absorben el parseo que hoy vive en `TelegramUpdateParser` y `MetaWebhookServiceImpl`.
- `MessagingChannelAdapterRegistry` → `get(ChannelType)` + `capabilitiesOf(ChannelType)`.
- `ChannelInboundMessageProcessor` → consume `InboundEvent`; la idempotencia pasa a apoyarse en el índice único (captura `DataIntegrityViolationException` → trata como duplicado, ya no hay carrera).
- `TelegramWebhookAutoRegister` / `TelegramWebhookBaseUrlResolver` → construyen la URL con `webhookToken`.
- `SecurityConfig` → `permitAll` sobre `/api/v1/crm/webhooks/**` (igual que hoy), pero la autenticidad pasa a depender de firma + token, no de la red.

Eliminar: `MetaWebhookController`, `TelegramWebhookController`, `MetaWebhookService(+Impl)`, `TelegramWebhookService`, `InboundWebhookMessageDto`.

**Cambio de contrato (webhooks):**

| Antes | Después |
|---|---|
| `POST /api/v1/crm/webhooks/telegram/{companyId}` + header `X-Telegram-Bot-Api-Secret-Token` | `POST /api/v1/crm/webhooks/TELEGRAM/{webhookToken}` (el header se sigue validando) |
| `GET  /api/v1/crm/webhooks/meta` | `GET  /api/v1/crm/webhooks/WHATSAPP/{webhookToken}` |
| `POST /api/v1/crm/webhooks/meta` (sin firma) | `POST /api/v1/crm/webhooks/WHATSAPP/{webhookToken}` (**exige `X-Hub-Signature-256` válida**) |

Acción manual tras desplegar: re-registrar el webhook de cada bot de Telegram y actualizar la callback URL en el Business Manager de Meta. El auto-registro de Telegram lo hace solo al arrancar en dev.

Tests: `ChannelWebhookControllerTest` (`@WebMvcTest`), `MetaSignatureVerifierTest` (firma válida/inválida/ausente), `TelegramAdapterParseTest`, `WhatsAppMetaAdapterParseTest` (lote con varios `entry`/`changes`), `WebhookIdempotencyTest` (mismo `provider_message_id` dos veces → una fila).

---

### Fase 3 — `TenantContext` + filtro Hibernate

Crear:

- `shared/tenant/TenantContext.java` — `ThreadLocal<Long>` + `runAs(companyId, Runnable)` para hilos de worker.
- `shared/tenant/TenantFilterAspect.java` — `@Aspect` que activa `session.enableFilter("tenantFilter").setParameter("companyId", ...)` al abrir la sesión.
- `shared/tenant/TenantContextFilter.java` — `OncePerRequestFilter` que puebla el contexto desde el usuario autenticado (y desde `X-Company-Id` sólo si es `SUPER_ADMIN`), y lo **limpia siempre** en `finally`.
- `shared/tenant/TenantScoped.java` — `@FilterDef` compartida.

Modificar: anotar con `@Filter(name="tenantFilter", condition="company_id = :companyId")` las entidades con `company_id` propio: `CrmContact`, `ChannelAccount`, `ContactIdentity`, `CrmContactPhone`, `Campaign`, `CampaignRecipient`, `MessageTemplate`, `CompanyModule`, `Appointment`. Para `Conversation` y `CrmMessage`, que no tienen `company_id`, añado la columna denormalizada en `V7__denormalize_company_id.sql` (backfill desde la conversación/contacto) — es la única forma de que el filtro las cubra sin JOIN.

- `ChannelWebhookController` y el worker de campañas entran **sin** usuario autenticado, así que fijan `TenantContext` explícitamente desde la cuenta de canal resuelta.
- Los `getAll()` de `ConversationController`, `MessageController` y `ChannelAccountController` quedan restringidos a `SUPER_ADMIN`.

Tests: `TenantFilterIntegrationTest` (empresa A no ve filas de B en queries derivadas y JPQL), `TenantContextFilterTest` (limpieza del ThreadLocal, `X-Company-Id` ignorado para no-super-admin).

---

### Fase 4 — Módulos por empresa

Crear:

- `modules/module/entity/AppModule.java`, `CompanyModule.java`, repositorios, `ModuleService(+Impl)`, `ModuleController` (`/api/v1/companies/{companyId}/modules`: `GET`, `PATCH /{code}/enable`, `PATCH /{code}/disable`).
- `shared/module/RequiresModule.java` — anotación a nivel de clase o método.
- `shared/module/ModuleAccessInterceptor.java` — `HandlerInterceptor`; lee `@RequiresModule` del `HandlerMethod`, consulta `CompanyModule` por el tenant activo (con caché en memoria), devuelve `403` con el shape de `ErrorResponse` si está desactivado.
- `shared/exception/ModuleDisabledException.java` + handler en `GlobalExceptionHandler`.
- Migración `V8__modules.sql` con catálogo inicial: `CRM_INBOX`, `CRM_CAMPAIGNS`, `CRM_WEBCHAT`, `APPOINTMENTS`, `EXTERNAL_DOCTORS`.

Modificar: `WebConfig` registra el interceptor. Anotar `ConversationController`/`MessageController` con `@RequiresModule("CRM_INBOX")`, campañas con `@RequiresModule("CRM_CAMPAIGNS")`.

**Decisión de compatibilidad:** la migración activa **todos** los módulos para las empresas existentes. Nadie pierde acceso al desplegar.

Tests: `ModuleServiceImplTest`, `ModuleAccessInterceptorTest`, `RequiresModuleIntegrationTest` (403 real con módulo apagado, 200 encendido).

---

### Fase 5 — Campañas (outbox)

Crear, en `modules/crm/campaign/`:

- Entidades `MessageTemplate`, `Campaign`, `CampaignRecipient` + enums `CampaignStatus`, `RecipientStatus`.
- Repositorios. `CampaignRecipientRepository.claimBatch` con

  ```sql
  SELECT * FROM campaign_recipients
   WHERE campaign_id = :id AND status = 'PENDING' AND next_attempt_at <= now()
   ORDER BY id FOR UPDATE SKIP LOCKED LIMIT :n
  ```

- `CampaignService(+Impl)` — crear, **materializar** destinatarios (resolviendo `ContactIdentity` por el `channel_type` de la cuenta; los contactos sin identidad quedan `SKIPPED` con motivo), programar, pausar, reanudar, cancelar.
- `CampaignDispatchWorker` — `@Scheduled(fixedDelay)`; por cada campaña `RUNNING` calcula el presupuesto del tick a partir de `min(campaign.rate_per_minute, account.throttle_per_minute)`, reclama ese lote, envía por el adapter, marca `SENT`/`FAILED`, y en fallo aplica backoff exponencial (`next_attempt_at = now + 2^attempts * base`, tope de 5 intentos).
- `CampaignRateLimiter` — ventana deslizante por `channel_account_id`, en memoria (asume **una sola instancia**; si vas a correr varias réplicas hay que moverlo a la BD — avísame y lo hago con una tabla de tokens).
- `TemplateRenderer` — sustituye `{{1}}` / `{{nombre}}` desde `variables_json`, falla si falta una variable requerida.
- Controllers `CampaignController` y `MessageTemplateController` bajo `/api/v1/crm/campaigns` y `/api/v1/crm/templates`.
- Migración `V9__campaigns.sql`.

Modificar: `IclinicBackendApplication` con `@EnableScheduling`. Los callbacks de estado (`statuses` de Meta) que ya llegan por webhook actualizan `campaign_recipients.status` a `DELIVERED`/`READ` cuando el `provider_message_id` corresponde a un destinatario.

Tests: `CampaignServiceImplTest` (materialización, contactos sin identidad → `SKIPPED`, idempotencia de materializar dos veces), `CampaignDispatchWorkerTest` (respeta el presupuesto del tick, backoff, tope de reintentos, campaña que termina), `TemplateRendererTest`, `CampaignRateLimiterTest`.

---

### Fase 6 — Limpieza

- Borrar rutas `@Deprecated` de `/api/v1/crm/channels/**`.
- Quitar el bypass `{plain}` y el fallback "legacy base64" de `SecretEncryptionService`; el arranque falla si `ENCRYPTION_SECRET_KEY` no está definida en perfil no-test. Migración de re-cifrado de los tokens `{plain}` existentes.
- Borrar `import.sql` y `src/main/resources/db/sql/` (sustituidos por migraciones).
- Retirar `CrmContact.phone` (ya deprecado a favor de `CrmContactPhone`) y `CrmContactServiceImpl.findOrCreate`.
- Actualizar `CLAUDE.md` y la sección CRM de `README.md`.

---

## 4. Resumen de impacto

**Sin cambios:** `/api/v1/appointments/**`, `/api/v1/users/**`, `/api/v1/branches/**`, `/api/v1/companies/**`, `/api/v1/auth/**`, `/api/v1/admin/**`, `/api/v1/external-doctor-access/**`.

Con una salvedad: en la Fase 3 esos endpoints empiezan a filtrar por tenant de verdad. Un `ADMIN` que hoy ve filas de otra empresa por un hueco de filtrado dejará de verlas. Es la corrección de un fallo, no una ruptura de contrato, pero cambia respuestas observables.

**Cambia:** todo `/api/v1/crm/**` (tablas de antes/después en las Fases 1 y 2).

**Requiere acción manual al desplegar:**

1. Levantar PostgreSQL; el perfil por defecto deja de ser H2.
2. Definir `ENCRYPTION_SECRET_KEY` (obligatoria desde la Fase 6).
3. Cargar el App Secret de Meta en cada cuenta de WhatsApp (`PUT /api/v1/crm/channel-accounts/{id}`).
4. Re-registrar webhooks de Telegram y actualizar la callback URL en Meta.
5. Revisar el reporte de contactos posiblemente fusionados entre tenants que emite `V4`.

**Orden de merge:** las fases son secuenciales; la 1 depende de la 0, la 2 de la 1, la 3 de la 1. Las fases 4 y 5 son independientes entre sí una vez está la 3.

---

## 5. Decisiones tomadas por defecto

No llegaron respuestas a las preguntas abiertas, así que se tomaron estos defaults.
Cualquiera es reversible; si alguno no te sirve, dilo y se cambia.

| # | Pregunta | Default aplicado |
|---|---|---|
| 1 | Unicidad de `contact_identities` | `UNIQUE (company_id, channel_type, external_id)` — evita la fuga entre tenants descrita en §0.2.2 |
| 2 | Tests del worker de campañas | Mocks + test de integración opcional contra Postgres. Sin Testcontainers |
| 3 | Perfil `h2` de desarrollo | **Reincorporado a petición tuya.** Se había eliminado por la restricción "ddl-auto sólo en el perfil de test". Vuelve como atajo para arrancar sin Docker, con la salvedad de que su esquema lo genera Hibernate y no las migraciones: no sirve para validar cambios de esquema |
| 4 | Réplicas de la app | Se asume **una sola instancia**. El rate limiter de campañas vivirá en memoria |
| 5 | Dependencias nuevas | Sólo Flyway (`flyway-core` + `flyway-database-postgresql`) |

---

## 6. Registro de avance

### Fase 0 — PostgreSQL + Flyway — COMPLETADA

Verificada contra PostgreSQL 16 real, no sólo compilada:

- `V1__baseline_schema.sql` aplicó correctamente sobre una base vacía (20 relaciones, `flyway_schema_history` en v1).
- **`ddl-auto=validate` pasó**, que es la prueba de que el baseline coincide con las entidades JPA.
- `dev-seed.sql` carga los mismos datos que el antiguo `import.sql` y es idempotente (segunda ejecución: 0 errores, sin duplicados).
- `./gradlew build`: **133 tests, 0 fallos**.

**Archivos creados**

| Archivo | Qué es |
|---|---|
| `src/main/resources/db/migration/V1__baseline_schema.sql` | Esquema baseline: 19 tablas, constraints con nombre estable, índices de FK y de las consultas existentes |
| `src/main/resources/db/seed/dev-seed.sql` | Datos de prueba para local. Fuera de la secuencia de Flyway, idempotente |
| `src/main/resources/application-dev.properties` | Perfil `dev`: seed + auto-registro de webhook Telegram |
| `src/test/resources/application-test.properties` | Perfil `test`: H2, único sitio con `ddl-auto` |
| `src/test/java/.../tools/SchemaExportTool.java` | Utilidad para regenerar el DDL desde las entidades (`./gradlew generateSchema`). Excluida de la suite |

**Archivos modificados**

| Archivo | Cambio |
|---|---|
| `build.gradle` | Flyway; `test` excluye `tools/`; tarea `generateSchema` |
| `application.properties` | Perfil por defecto `postgres`; `ddl-auto=validate`; Flyway activo; enums a `varchar` |
| `application-postgres.properties` | `validate` + Flyway; datasource por variables de entorno; sin seed |
| `application-h2.properties` | Eliminado y después **reincorporado** como perfil de arranque sin Docker (ver §5.3) |
| `config/SecurityConfig.java` | Abre `/api-docs` (la ruta real de springdoc). Antes sólo abría `/v3/api-docs/**`, así que Swagger UI cargaba pero no podía descargar el spec |

**Un hallazgo que no estaba en el plan:** `application.properties` fijaba
`spring.jpa.properties.hibernate.dialect=H2Dialect` de forma global, así que el
perfil `postgres` también corría con dialecto H2. Quitado — ahora se deduce de la
conexión. Esto por sí solo explicaría comportamientos raros del perfil postgres.

**Deuda que se saldó de paso:** la suite tenía 10 fallos previos a este trabajo.
Nueve eran bugs del propio test (fixtures sin campos `NOT NULL`, mocks apuntando a
un método que el impl ya no llama, asserts que asumían base vacía) y se corrigieron.
El décimo era una diferencia real de comportamiento: `CrmContactServiceImpl.findOrCreate`
ponía `"Desconocido"` como nombre de un contacto sin nombre, y el test esperaba el
teléfono. **Se cambió el código para usar el teléfono** (identifica mejor a la persona
en la bandeja, y el test era la única constancia escrita de la intención). Es un
cambio de comportamiento pequeño sobre un método ya deprecado: si prefieres
`"Desconocido"`, es revertir `resolveDisplayName`.

**Cómo probarlo**

```bash
docker compose up -d
./gradlew build                                        # 133 tests en verde

# Arranque limpio contra PostgreSQL (sin datos):
./gradlew bootRun

# Arranque con datos de prueba:
SPRING_PROFILES_ACTIVE=postgres,dev ./gradlew bootRun

# Verificar que Flyway aplicó y qué hay dentro:
docker exec iclinic-postgres psql -U postgres -d iclinic_db -c "select version, description, success from flyway_schema_history;"
docker exec iclinic-postgres psql -U postgres -d iclinic_db -c "\dt"

# Los endpoints siguen respondiendo igual que antes (403 sin Firebase configurado,
# que es el comportamiento previo, no una regresión):
curl -i http://localhost:8080/api/v1/companies
curl -i http://localhost:8080/swagger-ui/index.html
```

**Cómo cambiar de base:** una sola línea, `spring.profiles.active` en
`application.properties` (o la variable `SPRING_PROFILES_ACTIVE` sin tocar el archivo).
Valores: `postgres`, `postgres,dev`, `h2`.

**Detalle a tener presente:** DevTools activa la consola de H2 en cualquier perfil
de desarrollo, así que ver `/h2-console` respondiendo NO significa que estés sobre H2.
La señal fiable es el log de arranque: `PgConnection` vs `url=jdbc:h2:mem:iclinicdb`.

**Ojo al desplegar:** el perfil por defecto dejó de ser H2. `./gradlew bootRun` ahora
exige PostgreSQL levantado. Y `CLAUDE.md` describe el arranque antiguo; se actualiza
en la Fase 6.

### Fases pendientes

| Fase | Estado |
|---|---|
| 1 — `ChannelAccount` + `contact_identities` + conversación por cuenta | pendiente |
| 2 — Webhook genérico + Adapter nuevo + firma Meta | pendiente |
| 3 — `TenantContext` + filtro Hibernate | pendiente |
| 4 — Módulos por empresa | pendiente |
| 5 — Campañas (outbox) | pendiente |
| 6 — Limpieza | pendiente |
