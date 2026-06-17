# Plan Multitenant SaaS — iClinic

Documento de diseño que acompaña la auditoría. Describe (1) lo aplicado como MVP
defensivo y (2) el modelo SaaS objetivo con memberships + RBAC y su migración gradual.

---

## 1. MVP defensivo (ya aplicado sobre el esquema actual)

Cambios realizados y verificados (compila + tests tocados en verde):

| Archivo | Cambio |
|---------|--------|
| `application.properties` | Token Telegram y `encryption.secret-key` leídos de env (`TELEGRAM_BOT_TOKEN`, `ENCRYPTION_SECRET_KEY`). |
| `import.sql` | Eliminado token real de Telegram (placeholder de seed). |
| `config/SecurityConfig.java` | Mutaciones de `/api/v1/users/**` (POST/PUT/PATCH/DELETE) restringidas a `SUPER_ADMIN`/`ADMIN`. |
| `auth/service/CurrentUserService.java` | `getCurrentCompanyId()` + `assertBranchInCompany()`. |
| `user/service/UserServiceImpl.java` | Scoping tenant en `findAll/findByRole/findByCompanyId/findByBranchId/findDoctorsByBranchId/search`; `assertCanManageUser` en `create/deactivate/delete`; validación branch∈company en `assignRelations`. |
| `admin/service/AdminService.java` | `assertBranchInCompany` en `inviteUser`. |
| `appointment/service/AppointmentServiceImpl.java` | `assertCanAccessCompany` en `createAppointment` y `findByBranch`. |
| `auth/service/FirebaseAuthSyncService.java` | Fix: `createTemporaryUser` asigna `role=PATIENT` + `documentType` (antes violaba NOT NULL). |
| `user/entity/User.java` | `email` ahora `unique=true`. |

**Acciones manuales pendientes (no automatizables):**
- Rotar el token de Telegram en BotFather (el anterior quedó en el historial git).
- Definir `TELEGRAM_BOT_TOKEN` y `ENCRYPTION_SECRET_KEY` en el entorno de cada despliegue.
- Considerar purgar el secreto del historial git (`git filter-repo`).

**Pendiente P2 (documentado, no aplicado para no romper dev):**
- `h2-console` sigue con `permitAll`; condicionarlo a `@Profile("dev")` antes de prod.
- TODOs `assertCanAccessPatient/Conversation/Appointment` en `CurrentUserService` siguen vacíos; el filtrado tenant real de CRM/citas se completa con el modelo de memberships.

---

## 2. Modelo SaaS objetivo (entidades aditivas creadas)

Nuevas entidades en `modules/access/` (compilan y mapean; aún NO sustituyen a las
columnas de `users`):

- `CompanyMembership` (+ join `membership_branches`) — vínculo N:N user↔company con rol e `isOwner`.
- `Role`, `Permission` (+ join `role_permissions`) — RBAC con permisos finos.
- `AuditLog` — trazabilidad de acciones sensibles.

Repositorios: `CompanyMembershipRepository`, `RoleRepository`, `PermissionRepository`, `AuditLogRepository`.

### Estado final deseado de `users`
```
users(id, email UNIQUE, first_name, last_name, external_auth_id UNIQUE,
      auth_provider, photo_url, is_platform_admin BOOL, active)
-- SIN company_id, SIN branch_id, SIN role   (migrados a company_memberships)
```
- `SUPER_ADMIN` global → `is_platform_admin = true`, sin membership.
- Pacientes → permanecen en `crm_contacts`; se retira el rol `PATIENT` de `users`.

---

## 3. Migración gradual (sin big-bang)

**Fase A — Aditiva (actual).** Crear tablas nuevas. `users.company_id/branch_id/role`
siguen siendo la fuente de verdad. Sin cambios de comportamiento.

**Fase B — Backfill.** Script: por cada `user` con `company_id`, crear una
`company_membership(user, company, role, isOwner = (role==ADMIN?…))`; copiar
`branch_id` a `membership_branches`. Marcar `SUPER_ADMIN` como `is_platform_admin`.
```sql
INSERT INTO company_memberships (user_id, company_id, role, is_owner, active, created_at)
SELECT u.id, u.company_id, u.role,
       CASE WHEN u.role='ADMIN' THEN true ELSE false END, true, CURRENT_TIMESTAMP
FROM users u WHERE u.company_id IS NOT NULL AND u.role <> 'SUPER_ADMIN';
```

**Fase C — Doble lectura.** `CurrentUserService` resuelve empresa(s) desde
memberships con fallback a `users.company_id`. Validar paridad en logs.

**Fase D — Corte.** Cambiar escritura a memberships; eliminar lectura de columnas viejas.

**Fase E — Limpieza.** Drop de `users.company_id/branch_id/role` y del rol `PATIENT`.

---

## 4. Cableado de seguridad objetivo (RBAC)

1. `FirebaseAuthenticationFilter`: tras resolver el `User`, cargar sus memberships y
   exponer authorities `ROLE_<rol>` **+** `PERM_<code>` por empresa activa, o resolver
   el tenant del request (header `X-Company-Id` / path) y cargar sólo esa membership.
2. Endpoints con `@PreAuthorize("hasAuthority('PERM_APPOINTMENT_WRITE')")` en vez de
   `hasRole`.
3. `CurrentUserService.getCurrentCompanyIds()` (plural) y `assertCanAccessCompany`
   contra el conjunto de memberships.
4. Defensa en profundidad opcional: filtro Hibernate `@Filter(company_id = :tenant)`
   activado por request para que ninguna query devuelva datos de otra empresa.
5. Registrar en `AuditLog` toda creación/borrado de usuarios, cambios de rol/owner y
   accesos denegados (403 cross-tenant).

### Catálogo inicial de permisos sugerido
`USER_READ, USER_WRITE, USER_DELETE, APPOINTMENT_READ, APPOINTMENT_WRITE,
CRM_READ, CRM_WRITE, CHANNEL_MANAGE, COMPANY_MANAGE, BILLING_MANAGE`.

### Roles de sistema y reglas de invitación
- `OWNER` (todos los permisos de empresa) — uno obligatorio por empresa; no se puede
  quitar el último.
- `ADMIN` (todo salvo `BILLING_MANAGE`/borrar OWNER).
- `DENTIST`, `ASSISTANT`, `RECEPTIONIST` — subconjuntos.
- `OWNER`/`ADMIN` sólo invitan dentro de su empresa; nunca `SUPER_ADMIN` ni `OWNER`
  superior; nunca asignan sucursales de otra empresa.

---

## 5. Tests recomendados (siguiente entrega)
- `CompanyMembershipServiceTest`: único por (user,company); último OWNER no removible.
- Seguridad: ADMIN de empresa A recibe 403 al leer/crear en empresa B.
- `assertBranchInCompany`: branch de otra empresa → 400.
- RBAC slice: endpoint protegido por permiso responde 403 sin la authority.
- Backfill: paridad memberships vs columnas legacy.
