# Keycloak

## Por qué los ficheros de realm no llevan comentarios

El importador de Keycloak deserializa el JSON a `RealmRepresentation` **sin
ignorar campos desconocidos**. Una clave inventada, aunque empiece por guion bajo,
aborta el arranque entero:

```
ERROR: Failed to run import
ERROR: Unrecognized field "_comentario" (class ...RealmRepresentation),
       not marked as ignorable
```

Y el contenedor sale con código 1. Por eso las notas viven aquí y no dentro del
JSON.

## `realm-iclinic.json` — realm de desarrollo

Se importa al arrancar `docker compose up keycloak`. Puntos que no se ven leyendo
el fichero:

**El mapper de audiencia de `iclinic-api` no es opcional.** Sin él, el token que
emite `iclinic-web` no lleva `iclinic-api` en `aud`, y `AudienceValidator` lo
rechaza con 401. Es el fallo de configuración más común al montar esto, y el
síntoma —todo responde 401 con un token que parece correcto— no apunta a la
causa.

**Los nombres de rol son los del enum `UserRole` que ya existe**, más
`BRANCH_MANAGER` y `BILLING`. Es `DENTIST`, no `DOCTOR`; es `ADMIN`, no
`ORG_ADMIN`. Así la migración de roles es literal y no hace falta tabla de
traducción.

**El flujo `mfa-condicional` queda montado pero sin parametrizar.** La condición
de `conditional-user-role` se configura por ejecución, una por rol, y eso no cabe
en el import: se aplica con `kcadm` después de importar. Mientras no se haga, el
OTP no se exige a nadie.

**Los UUID de los usuarios son fijos y coinciden** con `keycloak_user_id` en
`db/seed/dev-seed.sql` e `import.sql`, para que el entorno local funcione sin
pasos manuales. La contraseña `dev` es solo para desarrollo: este realm no se
despliega.

**`start-dev` no vale para producción**: sin HTTPS, sin caché distribuida y
reimportando el realm en cada arranque. Para desplegar se usa `start` con
`KC_HOSTNAME` y certificados.

## `src/test/resources/keycloak/realm-iclinic-test.json` — realm de los tests

Mínimo, para `KeycloakAuthenticationIT`. Difiere del de desarrollo en una sola
cosa deliberada: aquí los clientes tienen `directAccessGrantsEnabled`, para poder
pedir un token con usuario y contraseña desde el test. En el realm real ese flujo
está apagado, porque permite probar contraseñas sin pasar por el navegador.

**`otro-cliente-test` no tiene el mapper de audiencia, y eso es el test.** Emite
un token del mismo realm, con la misma firma y el mismo emisor, pero sin
`iclinic-api` en `aud`. Es justo lo que `AudienceValidator` tiene que rechazar. Si
alguien le añade el mapper, el test de audiencia deja de probar nada y pasa en
verde.

## El endpoint de salud no está donde se espera

Desde Keycloak 25, `/health` vive en la **interfaz de gestión (puerto 9000)**, no
en el 8080. Las estrategias de espera que apuntan a `/health/started` en el puerto
HTTP agotan el tiempo aunque el servidor esté en pie. `KeycloakAuthenticationIT`
espera contra `/realms/iclinic`, que además comprueba lo que de verdad importa:
que el realm se importó.

## Puesta en marcha local

```bash
docker compose up -d keycloak
# Consola: http://localhost:8081  (admin / admin)
```

Para el alta de los usuarios ya existentes, ver `migrar-usuarios.md`.
