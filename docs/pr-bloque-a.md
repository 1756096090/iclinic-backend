# Bloque A: Keycloak sustituye a Firebase

La identidad sale de la base de datos. `users` deja de ser una credencial y pasa a
ser la proyección del sujeto de Keycloak.

Reparto: **Keycloak** dice quién eres y qué rol general tienes. **PostgreSQL**
guarda el dominio. **Spring Boot** decide si puedes ejercer ese rol aquí.

## Qué entra

**Identidad.** Fuera `firebase-admin` y las cuatro clases de Firebase; dentro
`spring-boot-starter-oauth2-resource-server`. `users` gana `keycloak_user_id`,
`subject_type`, `keycloak_client_id` y `tokens_valid_from`.

**Tres piezas que Spring no da hechas:**

- `KeycloakRolesConverter` lee `resource_access.iclinic-api.roles`. El conversor
  por defecto lee `scope`, que es otra cosa: sin esto el token llega sin
  autoridades y todo responde 403.
- `AudienceValidator`. El emisor es el mismo para todos los clientes del realm, así
  que sin validar `aud` un token emitido para otro cliente entra como propio.
- `TokenRevocationFilter`, el interruptor de emergencia: rechaza todo token con
  `iat` anterior a `tokens_valid_from`. Un `UPDATE` corta todas las sesiones vivas
  de una persona en la siguiente petición, sin estado de sesión en el backend.

**`anyRequest().denyAll()`** en lugar de `authenticated()`: un controlador nuevo sin
regla debe dar 403, no nacer abierto.

**Infraestructura.** Keycloak en `docker-compose` con su propia base. Realm
versionado en `infra/keycloak/realm-iclinic.json`, con los nueve roles de cliente,
el mapper de audiencia y el OTP condicional.

**Migración.** `infra/keycloak/migrar-usuarios.sh`, idempotente, por lotes y con
`--company-id`.

## Cómo adoptar el baseline en un entorno ya desplegado

El repositorio ya tenía `V1__baseline_schema.sql` cuando empezó este bloque. Sobre
una base que hoy existe y **no** tiene `flyway_schema_history`:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

Con eso Flyway adopta el esquema existente como `V1` sin volver a crearlo, y aplica
`V2` y `V3` a continuación. Sobre una base vacía no hace falta: las tres se aplican
en orden.

Orden de despliegue, y **no es intercambiable**:

1. Desplegar el código con `V2` aplicada. `V2` solo **añade** columnas, así que la
   versión anterior de la aplicación sigue funcionando: se puede desplegar sin
   parar nada.
2. Dar de alta a los usuarios en Keycloak y rellenar `keycloak_user_id` con
   `migrar-usuarios.sh`. Ver más abajo.
3. Solo entonces aplicar `V3`, que retira `external_auth_id`, `auth_provider` y
   `password`.

`V3` **aborta y lista los afectados** si queda algún usuario activo sin
`keycloak_user_id`. Es deliberado: mejor una migración que falla que un usuario que
no puede volver a entrar.

Comprobación previa a `V3`:

```sql
SELECT count(*) FROM users WHERE keycloak_user_id IS NULL AND active = true;
```

## Los usuarios tendrán que establecer contraseña

**Esto es visible para ellos y hay que avisar antes.**

Nadie tiene una contraseña migrable: lo que había en `users.password` eran hashes
BCrypt que el código ya no comprobaba, porque la autenticación real era Firebase.
Al pasar a Keycloak, cada persona recibe la acción requerida `UPDATE_PASSWORD` y la
establece en su primer acceso.

Recomendación: hacerlo **por empresa**, no para todos a la vez.

```bash
./infra/keycloak/migrar-usuarios.sh --company-id 1 --dry-run
./infra/keycloak/migrar-usuarios.sh --company-id 1
```

El guion es idempotente: se puede interrumpir y relanzar.

## Cambios de contrato

| Antes | Ahora |
|---|---|
| `POST /api/v1/auth/firebase/sync` | Desaparece |
| `POST /api/v1/users` con `password` | Sin `password`: la credencial vive en Keycloak |
| `AuthUserResponseDto.externalAuthId` / `.authProvider` | `keycloakUserId` (UUID) |

El detalle para el frontend está en `docs/frontend-keycloak.md`: issuer, PKCE sin
secret, dónde guardar cada token, TTLs, y qué hacer con 401, 403 y 404.

## Verificación

- `./gradlew build` — **134 tests, 0 fallos**.
- `./gradlew integrationTest` — **12 tests, 0 fallos**, contra Keycloak 26 y
  PostgreSQL 16 reales con Testcontainers.
- Flyway `V1`–`V3` aplican sobre base vacía y `ddl-auto=validate` pasa.
- **Prueba de mutación**: quitando `TokenRevocationFilter` de la cadena, el test de
  revocación falla. Se comprobó en los dos sentidos.

`integrationTest` va en su propia tarea para que `./gradlew test` siga corriendo sin
Docker.

## Defectos encontrados al ejecutar, no al escribir

Todos pasaban desapercibidos con la suite en verde:

1. **`TokenRevocationFilter` no surtía efecto.** Estaba tras
   `UsernamePasswordAuthenticationFilter`, que en la cadena va *antes* de
   `BearerTokenAuthenticationFilter`: corría con el contexto vacío.
2. **Y aun así el test pasaba.** Al declararlo como `@Bean`, Spring Boot lo
   registraba *además* en la cadena de servlets global, y esa registración
   accidental —aplicada a todas las rutas, incluidas las públicas— era la que
   protegía. Desactivada con un `FilterRegistrationBean`.
3. **Declarar `clientScopes` en el import sustituye a los de Keycloak.** El realm se
   quedaba sin `roles`, y sin ese scope el token no lleva `resource_access`: todo
   responde 403 con un token válido.
4. **El mapper de audiencia estaba en `iclinic-api`**, que es bearer-only y no emite
   tokens. Va en `iclinic-web`.
5. **El guion de migración desplazaba columnas** con un `last_name` vacío: el
   tabulador es espacio en blanco para `IFS` y bash colapsa los consecutivos. Se usa
   el separador de unidad 0x1F.
6. **El importador de Keycloak no ignora campos desconocidos.** Tumbó el arranque
   tres veces. Por eso `RealmDeProduccionIT` valida el realm que se despliega en
   cada build.

## Qué NO entra

- Autorización a nivel de objeto: sigue sin haber `@PreAuthorize`. El IDOR se cierra
  en el Bloque D.
- Eliminación del perfil `h2` y de `users.company_id`: van en el Bloque B.
- `timestamptz`: PR propio, inmediatamente después de éste.
