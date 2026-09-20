# Alta de los usuarios existentes en Keycloak

Se ejecuta **entre `V2` y `V3`**. `V2` añade `keycloak_user_id`; este proceso lo
rellena; `V3` retira `external_auth_id`, `auth_provider` y `password`, y **aborta
si queda algún usuario activo sin proyectar**.

Ese orden no es casual: si se retiraran las credenciales antes de dar de alta a la
gente en Keycloak, quedarían sin poder entrar y sin forma de volver atrás.

## Lo que hay que decirle al equipo antes

Nadie tiene contraseña en nuestro lado que se pueda migrar: lo que hay en
`users.password` son hashes BCrypt que el código ya no comprueba, y la
autenticación real era Firebase. Al pasar a Keycloak **todo el mundo tiene que
establecer contraseña de nuevo**.

El proceso da de alta a cada usuario con la acción requerida
`UPDATE_PASSWORD`, así que en su primer acceso Keycloak se la pide. Conviene
avisar antes, no después.

## Requisitos

- Keycloak accesible y el realm `iclinic` importado.
- Un cliente de administración con permiso `manage-users` sobre el realm.
- Acceso de lectura a la base de la aplicación.

## Ejecución

```bash
export KEYCLOAK_URL=https://auth.ejemplo.com
export KEYCLOAK_REALM=iclinic
export KEYCLOAK_ADMIN_CLIENT_ID=iclinic-migrador
export KEYCLOAK_ADMIN_CLIENT_SECRET=...
export DB_URL="postgresql://usuario@host:5432/iclinic_db"

./infra/keycloak/migrar-usuarios.sh --lote 50 --dry-run   # primero en seco
./infra/keycloak/migrar-usuarios.sh --lote 50
```

**Es idempotente.** Se puede interrumpir y relanzar: para cada usuario busca
primero por correo en Keycloak y solo crea si no existe. Si ya tiene
`keycloak_user_id` en la base, lo salta.

**Va por lotes** (`--lote`, 50 por defecto) porque la Admin API de Keycloak
responde de una en una y con varios miles de usuarios una única pasada se hace
larga y frágil.

## Qué hace por cada usuario

1. Lo busca en Keycloak por correo.
2. Si no existe, lo crea con: correo, nombre, apellidos, `enabled` según
   `users.active`, `emailVerified = false` y la acción requerida
   `UPDATE_PASSWORD`.
3. Le asigna el *client role* de `iclinic-api` que corresponde a `users.role`. El
   enum es el mismo, así que el mapeo es literal y no hay traducción.
4. Escribe el `id` devuelto en `users.keycloak_user_id`.

## Comprobación antes de aplicar V3

```sql
SELECT count(*) AS sin_proyectar
  FROM users
 WHERE keycloak_user_id IS NULL
   AND active = true;
```

Tiene que devolver cero. Si no, `V3` falla a propósito y lista los afectados.

## Vuelta atrás

Mientras `V3` no se haya aplicado, revertir es dejar de usar el filtro nuevo: las
columnas de Firebase siguen ahí. Después de `V3` no hay vuelta atrás sin volver a
pedir a los usuarios que establezcan contraseña, porque los hashes se eliminaron
y no deben restaurarse.
