# Autenticación: de Firebase a Keycloak

Qué tiene que cambiar en el frontend. Todo lo de aquí está verificado contra el
backend de la rama `feat/bloque-a-keycloak` y un Keycloak 26 real.

## Qué cambia

| Antes | Ahora |
|---|---|
| SDK de Firebase → `idToken` → `POST /api/v1/auth/firebase/sync` | OIDC Authorization Code + PKCE contra Keycloak → access token → `Authorization: Bearer` |

El endpoint `/api/v1/auth/firebase/sync` **ya no existe**.

## Configuración

```
issuer:    http://localhost:8081/realms/iclinic     (prod: https://auth.<dominio>/realms/iclinic)
clientId:  iclinic-web
scopes:    openid profile email
redirect:  <origen>/callback
PKCE:      obligatorio (S256)
```

El cliente es **público**: no hay client secret, y no debe haberlo. Un secreto en
una SPA no es un secreto.

Librería sugerida: `oidc-client-ts`, o `angular-auth-oidc-client` /
`react-oidc-context`.

**No pidas `scope=iclinic-api-audience`.** El claim `aud` llega solo: lo añade un
*protocol mapper* del propio cliente, no un scope que haya que solicitar. Se
comprobó pidiendo un token sin ningún scope extra:

```
aud   : iclinic-api
scope : profile email
roles : ['DENTIST']
```

Si tuvieras que pedir un scope para que apareciera `aud`, la seguridad
dependería de que el frontend se acuerde, y el día que alguien refactorice la
llamada todo respondería 401 sin motivo aparente.

## Dónde se guardan los tokens

| Token | Dónde | Por qué |
|---|---|---|
| access | **En memoria** | Con datos clínicos, un XSS que lea `localStorage` se lleva la sesión entera |
| refresh | Cookie `HttpOnly` + `Secure` + `SameSite=Strict`, o el almacenamiento cifrado de la librería | Nunca accesible desde el JS de la aplicación |

## Ciclo de vida

- Access token: **5 minutos**. Renueva en silencio ~1 minuto antes de expirar.
- Refresh: **8 h de inactividad / 12 h absoluto**. Al agotarse, al login.
- Los *offline tokens* están deshabilitados en el realm.

## Códigos de error y qué hacer con cada uno

| Respuesta | Qué significa | Qué hacer |
|---|---|---|
| `401` + `WWW-Authenticate: Bearer error="invalid_token"` | Token expirado o inválido | Renueva; si falla, al login |
| `401` + `error="insufficient_user_authentication"`, `max_age="300"` | Hace falta reautenticación reciente | Relanza el login con `max_age=300` y vuelve. Firmar historias, exportar datos y conceder accesos lo exigirán |
| `401` inesperado con un token que no ha expirado | La sesión fue **revocada en servidor** (`tokens_valid_from`) | Al login, **sin reintentar**: reintentar entra en bucle |
| `403` | Autenticado, pero sin permiso sobre **ese** objeto | No reintentes. Muestra el mensaje y, si aplica, la vía para pedir acceso |
| `404` en un recurso que "debería existir" | Es de otra clínica | Trátalo como inexistente. **No muestres «sin permiso»**: eso confirmaría que existe en otra empresa |

Esa última distinción entre 403 y 404 es deliberada, no un descuido: un 403 sobre
un recurso ajeno revela que el paciente existe en otra clínica.

## Roles en el token

```
resource_access["iclinic-api"].roles
```

Valores: `SUPER_ADMIN`, `ADMIN`, `BRANCH_MANAGER`, `DENTIST`, `ASSISTANT`,
`RECEPTIONIST`, `EXTERNAL_DOCTOR`, `BILLING`, `PATIENT`.

Úsalos **solo para ocultar botones**. La autorización real la hace el backend, y
el rol del JWT es un **techo, no un permiso**: el backend lo cruza con el rol que
ese usuario tiene en *esa* empresa. Un `DENTIST` en el token puede no serlo en la
clínica sobre la que está operando.

## MFA

`SUPER_ADMIN`, `ADMIN` y `DENTIST` pasan por OTP. Como el flujo es de navegador,
el frontend no tiene que hacer nada especial: Keycloak muestra la pantalla dentro
del *authorization code flow*. Lo único que cambia es que el login puede tardar
un paso más; no asumas que la redirección vuelve inmediatamente.

## Multi-empresa

**El token no lleva la empresa.** Manda la cabecera `X-Organization-Id` (o el id
en la ruta) en cada petición. Si el usuario pertenece a varias, hay que elegir una
al entrar y poder cambiarla.

Es deliberado: con la empresa dentro del token, revocar una membresía no tendría
efecto hasta que el token expirase.

## Qué borrar del frontend

- `firebase` / `firebase-auth` y su inicialización.
- La llamada a `/api/v1/auth/firebase/sync`.
- Cualquier lectura del `idToken` de Firebase.

## Cambios de contrato en la API

- `POST /api/v1/users` deja de aceptar `password`: la credencial vive en Keycloak.
- `AuthUserResponseDto` cambia `externalAuthId` y `authProvider` por
  `keycloakUserId` (UUID).
- `GET /api/v1/auth/me` mantiene la ruta; cambia el origen de la identidad.

## Para probar en local

```bash
docker compose up -d postgres keycloak
# Keycloak: http://localhost:8081   (admin / admin)
# Usuarios de desarrollo, contraseña `dev`:
#   juan.garcia@clinica.ec       ADMIN
#   maria.rodriguez@clinica.ec   DENTIST
#   carlos.mendoza@clinica.ec    RECEPTIONIST
#   isaaccs2003@gmail.com        SUPER_ADMIN
```

El *password grant* está apagado en `iclinic-web` a propósito: para obtener un
token hay que pasar por el navegador, como en producción.
