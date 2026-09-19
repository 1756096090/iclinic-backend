#!/usr/bin/env bash
#
# Alta idempotente y por lotes de los usuarios existentes en Keycloak.
# Ver migrar-usuarios.md para el contexto y el orden respecto a V2 y V3.
#
set -euo pipefail

LOTE=50
DRY_RUN=0
COMPANY_ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --lote)       LOTE="$2"; shift 2 ;;
    --company-id) COMPANY_ID="$2"; shift 2 ;;
    --dry-run)    DRY_RUN=1; shift ;;
    *) echo "Uso: $0 [--lote N] [--company-id N] [--dry-run]" >&2; exit 2 ;;
  esac
done

# Migrar por empresa, no todos de golpe: al entrar en Keycloak cada persona tiene
# que establecer contrasena, y eso se coordina clinica por clinica.
FILTRO_EMPRESA=""
if [[ -n "${COMPANY_ID}" ]]; then
  [[ "${COMPANY_ID}" =~ ^[0-9]+$ ]] || { echo "--company-id debe ser numerico" >&2; exit 2; }
  FILTRO_EMPRESA="AND EXISTS (SELECT 1 FROM company_memberships m
                                WHERE m.user_id = users.id AND m.company_id = ${COMPANY_ID})"
fi

: "${KEYCLOAK_URL:?falta KEYCLOAK_URL}"
: "${KEYCLOAK_REALM:?falta KEYCLOAK_REALM}"
: "${KEYCLOAK_ADMIN_CLIENT_ID:?falta KEYCLOAK_ADMIN_CLIENT_ID}"
: "${KEYCLOAK_ADMIN_CLIENT_SECRET:?falta KEYCLOAK_ADMIN_CLIENT_SECRET}"
: "${DB_URL:?falta DB_URL}"

command -v jq  >/dev/null || { echo "hace falta jq" >&2; exit 1; }
command -v psql >/dev/null || { echo "hace falta psql" >&2; exit 1; }

API="${KEYCLOAK_URL}/admin/realms/${KEYCLOAK_REALM}"

obtener_token() {
  curl -sS --fail-with-body \
    -d "grant_type=client_credentials" \
    -d "client_id=${KEYCLOAK_ADMIN_CLIENT_ID}" \
    -d "client_secret=${KEYCLOAK_ADMIN_CLIENT_SECRET}" \
    "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
    | jq -r .access_token
}

TOKEN="$(obtener_token)"
auth=(-H "Authorization: Bearer ${TOKEN}")

# id interno del cliente iclinic-api, necesario para asignar client roles
CLIENT_UUID="$(curl -sS --fail-with-body "${auth[@]}" \
  "${API}/clients?clientId=iclinic-api" | jq -r '.[0].id')"
[[ "${CLIENT_UUID}" != "null" && -n "${CLIENT_UUID}" ]] || {
  echo "No existe el cliente iclinic-api en el realm ${KEYCLOAK_REALM}" >&2; exit 1; }

creados=0; reutilizados=0; fallidos=0

while :; do
  # Solo los que faltan. Al escribir keycloak_user_id al final de cada iteracion,
  # relanzar el script retoma donde se quedo: de ahi la idempotencia.
  # Separador de unidad (0x1F), NO tabulador: el tabulador cuenta como espacio
  # en blanco para IFS y bash colapsa los consecutivos en uno solo. Con un
  # last_name vacio las columnas se DESPLAZAN y se acaba pidiendo el rol "t"
  # —el valor de `active`—, que devuelve 404. Detectado con un usuario sin apellido.
  filas="$(psql "${DB_URL}" -At -F $'\x1f' -c \
    "SELECT id, email, first_name, last_name, role, active
       FROM users
      WHERE keycloak_user_id IS NULL
        ${FILTRO_EMPRESA}
      ORDER BY id
      LIMIT ${LOTE}")"

  [[ -z "${filas}" ]] && break

  while IFS=$'\x1f' read -r id email nombre apellidos rol activo; do
    [[ -z "${id}" ]] && continue

    existente="$(curl -sS --fail-with-body "${auth[@]}" \
      --get --data-urlencode "email=${email}" --data-urlencode "exact=true" \
      "${API}/users" | jq -r '.[0].id // empty')"

    if [[ -n "${existente}" ]]; then
      kc_id="${existente}"
      reutilizados=$((reutilizados + 1))
    else
      if [[ "${DRY_RUN}" == "1" ]]; then
        echo "[seco] crearia ${email} con rol ${rol}"
        continue
      fi
      habilitado=$([[ "${activo}" == "t" ]] && echo true || echo false)
      cuerpo="$(jq -n \
        --arg email "${email}" --arg nombre "${nombre}" \
        --arg apellidos "${apellidos}" --argjson habilitado "${habilitado}" \
        '{username:$email, email:$email, firstName:$nombre, lastName:$apellidos,
          enabled:$habilitado, emailVerified:false,
          requiredActions:["UPDATE_PASSWORD"]}')"

      if ! respuesta="$(curl -sS --fail-with-body -o /dev/null -D - -w '%{http_code}' \
            "${auth[@]}" -H 'Content-Type: application/json' \
            -d "${cuerpo}" "${API}/users" 2>&1)"; then
        echo "FALLO al crear ${email}: ${respuesta}" >&2
        fallidos=$((fallidos + 1))
        continue
      fi
      kc_id="$(curl -sS --fail-with-body "${auth[@]}" \
        --get --data-urlencode "email=${email}" --data-urlencode "exact=true" \
        "${API}/users" | jq -r '.[0].id')"
      creados=$((creados + 1))
    fi

    # El enum de roles es el mismo a los dos lados, asi que el mapeo es literal.
    rol_json="$(curl -sS --fail-with-body "${auth[@]}" \
      "${API}/clients/${CLIENT_UUID}/roles/${rol}")"
    curl -sS --fail-with-body -o /dev/null "${auth[@]}" \
      -H 'Content-Type: application/json' \
      -d "[${rol_json}]" \
      "${API}/users/${kc_id}/role-mappings/clients/${CLIENT_UUID}"

    [[ "${DRY_RUN}" == "1" ]] && continue
    psql "${DB_URL}" -q -c \
      "UPDATE users SET keycloak_user_id = '${kc_id}' WHERE id = ${id}"
  done <<< "${filas}"

  [[ "${DRY_RUN}" == "1" ]] && break
done

echo "creados=${creados} reutilizados=${reutilizados} fallidos=${fallidos}"

pendientes="$(psql "${DB_URL}" -At -c \
  "SELECT count(*) FROM users WHERE keycloak_user_id IS NULL AND active = true ${FILTRO_EMPRESA}")"
echo "usuarios activos sin proyectar: ${pendientes}"
if [[ "${pendientes}" != "0" ]]; then
  echo "V3 fallara mientras esto no sea 0." >&2
  exit 1
fi
