# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

iClinic Backend is a Spring Boot REST API for a multi-country (Ecuador, Colombia, Peru) dental clinic platform. It combines appointment scheduling with an omnichannel CRM (Telegram + WhatsApp). Spring Boot 3.5.13-SNAPSHOT, Java 17, Gradle.

## Commands

```bash
./gradlew bootRun                  # Run app (default profile: h2, in-memory DB)
./gradlew build                    # Compile + run all tests
./gradlew test                     # Run all tests
./gradlew test --tests "com.iclinic.iclinicbackend.modules.appointment.service.AppointmentServiceImplTest"   # Single test class
./gradlew test --tests "*AppointmentServiceImplTest.shouldCreateAppointment"                                  # Single test method
./gradlew clean build
```

Run with PostgreSQL instead of H2:
```bash
docker-compose up -d                                    # Start postgres:16 on :5432 (db iclinic_db)
SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun       # Or set spring.profiles.active=postgres
```

### Key URLs (dev)
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 console: http://localhost:8080/h2-console — JDBC `jdbc:h2:mem:iclinicdb`, user `sa`, no password

## Architecture

### Module layout
Code lives under `src/main/java/com/iclinic/iclinicbackend/`, organized by **feature module** (not by layer). Each module under `modules/` follows the same internal structure: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, plus module-specific `enums/`, `factory/`, `strategy/`, `adapter/` where applicable.

Modules: `company`, `branch`, `user`, `appointment`, `auth`, `admin`, and `crm` (which itself nests `contact`, `conversation`, `message`, `channel`, `webhook`). Cross-cutting code is in `config/` (Spring `@Configuration` beans) and `shared/` (`entity/` base classes, `enums/`, `exception/`, `security/`, `validation/`, `utils/`).

### Multi-country entity inheritance
Country-specific behavior is modeled with JPA inheritance, not flags:
- **Companies**: `Company` (abstract) → `EcuadorianCompany` (RUC), `ColombianCompany` (NIT).
- **Users**: `User` (abstract) → `EcuadorianUser`, `ColombianUser`, `PeruvianUser`, `InternationalUser`. When persisting users always use a concrete subclass (e.g. `EcuadorianUser`), never the abstract `User`.

Creating a company goes through the **Strategy + Registry + Factory** chain: `CompanyCreationStrategyRegistry` selects a `CompanyCreationStrategy` (one impl per country) by `CompanyType`; `CompanyFactory` builds the concrete entity. To add a country, add a strategy implementation and a concrete entity — the registry auto-discovers it via Spring injection. `branch/factory/` follows the same factory pattern for `BranchType` (clinic/hospital).

### CRM omnichannel adapters
Messaging providers are abstracted behind `MessagingChannelAdapter`. Implementations: `TelegramAdapter`, `WhatsAppMetaAdapter`. `MessagingChannelAdapterRegistry` auto-discovers adapters and routes by `ChannelProvider`/`ChannelType`. Provider HTTP clients (`TelegramClient`, `MetaWhatsAppClient`) live alongside each adapter and use the shared `RestTemplate` from `RestTemplateConfig`. Inbound provider messages arrive at `/api/v1/crm/webhooks/**` (public, no auth) and are processed in `crm/webhook/service/`. Channel secrets/tokens are encrypted at rest via `SecretEncryptionService` (AES-256/GCM, key from `encryption.secret-key`).

### Authentication & authorization
Auth is **Firebase ID token based**, enforced in `FirebaseAuthenticationFilter` (registered in `SecurityConfig`). The filter reads `Authorization: Bearer <firebase-id-token>`, verifies it, then looks up the local `User` by `externalAuthId` (Firebase UID) and sets a Spring `ROLE_<UserRole>` authority. New Firebase users are reconciled to local users via `FirebaseAuthSyncService` (the public `/api/v1/auth/firebase/sync` endpoint).

Firebase is **optional in dev**: `FirebaseConfig` and `SecurityConfig` use `Optional<FirebaseAuth>` / `@ConditionalOnBean`, so the app starts without `secrets/firebase-service-account.json` (auth filter is simply not registered). Route authorization rules live in `SecurityConfig.filterChain`; roles are defined in `shared/enums/UserRole` (SUPER_ADMIN, ADMIN, DENTIST, ASSISTANT, RECEPTIONIST, EXTERNAL_DOCTOR, PATIENT).

### Conventions
- **Mappers** convert Entity↔DTO per module (`*/mapper/`); controllers/services never expose entities directly.
- **Error handling** is centralized in `shared/exception/GlobalExceptionHandler` (`@ControllerAdvice`), returning the `ErrorResponse` shape. Throw domain exceptions (e.g. `CompanyNotFoundException`, `LastSuperAdminException`) rather than building responses inline.
- **Base entities** in `shared/entity/`: `BaseEntity` (id/timestamps) and `ActivableEntity` (soft-activation `active` flag). Deactivation is done via PATCH `/deactivate` endpoints, not hard delete.
- Lombok is used throughout (`@RequiredArgsConstructor` for constructor injection, `@Slf4j` for logging).

## Database & seed data
- DDL is Hibernate-generated. h2 profile uses `create-drop`; postgres profile uses `update`.
- Seed data is `src/main/resources/import.sql`, auto-loaded after DDL on H2 (`spring.jpa.defer-datasource-initialization=true`). It preloads companies, branches, users, doctor schedules (`branch_schedules`), and CRM contacts/messages. When adding columns with NOT NULL constraints (e.g. `UPDATED_AT`), update the matching INSERTs in `import.sql` or startup fails.
- PostgreSQL-compatible seed scripts exist under `classpath:db/sql/` (commented out in `application-postgres.properties`).

## Notes
- The repo root contains many `AD *.md` / Spanish documentation files and `app.log`; treat `README.md` as the canonical (Spanish) functional doc. Most docs and code comments are in Spanish.
- Telegram bot config and the `encryption.secret-key` are currently hardcoded in `application.properties` for dev — use environment variables in production.
