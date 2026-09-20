# 001 — Tareas

Borrador. Orden de ejecución de `plan.md`. Cada tarea es pequeña, verificable por
sí sola y deja el repositorio compilando con la suite en verde. Dentro de cada
tarea, los tests se escriben antes que la implementación y deben fallar por la
razón correcta antes de escribir el código.

Convención de commits: `<tipo>(<ámbito>): <qué>`, en español, sin referencia a
porcentajes de avance. Los tipos en uso: `feat`, `fix`, `refactor`, `test`,
`chore`, `docs`.

Ninguna tarea se da por hecha sin la verificación obligatoria de `AGENTS.md`.

## Bloque 0 — Medición previa

Sin esta medición, tres migraciones posteriores se escriben a ciegas.

### T-00. Inventariar los datos que violarían las restricciones nuevas

Consultas de solo lectura sobre una base con datos reales o representativos, para
contar: identidades de canal con el mismo identificador externo en empresas
distintas; contactos alcanzables desde más de una empresa; conversaciones abiertas
duplicadas por contacto y cuenta; mensajes con el mismo identificador de proveedor.

Hecho cuando: existe un documento con los recuentos y, para cada caso con recuento
mayor que cero, la decisión tomada sobre qué hace la migración.

Commit: `docs(crm): inventario de datos previo a la migración multi-cuenta`

Bloqueada por: [NECESITA ACLARACIÓN: contra qué base se mide. En local solo está
`dev-seed.sql`, que no contiene los casos problemáticos. Hace falta acceso a datos
reales o la confirmación de que no hay entorno con datos en uso.]

## Bloque 1 — La cuenta se identifica a sí misma

### T-01. Identificador de webhook en la cuenta de canal

Tests: el alta de una cuenta genera un identificador de webhook no nulo y distinto
del de otra cuenta; la respuesta del alta incluye la URL de webhook y no incluye
credenciales.

Implementación: columna e identificador en la entidad, generación al crear,
migración con relleno para las filas existentes, exposición en el DTO de
respuesta.

Hecho cuando: la migración aplica sobre una base con `dev-seed.sql` cargado, todas
las filas tienen identificador, `ddl-auto=validate` pasa y la suite está en verde.

Commit: `feat(crm): identificador de webhook por cuenta de canal`

### T-02. Secreto de aplicación y datos de cuenta de Meta

Tests: el secreto se guarda cifrado y no aparece en ninguna respuesta; una cuenta
de Meta sin secreto de aplicación es rechazada al darse de alta.

Implementación: columnas de secreto de aplicación cifrado, identificador de cuenta
de negocio y nombre visible; migración; validación en el alta.

Hecho cuando: existe un test que demuestra que el valor persistido no es el valor
en claro.

Commit: `feat(crm): secreto de aplicación y nombre visible en la cuenta de canal`

### T-03. Unicidad de cuenta por empresa y global

Tests: RF-03 (segunda cuenta con el mismo identificador externo en la misma
empresa es rechazada) y RF-04 (mismo identificador externo en otra empresa es
rechazado), ambos a nivel de servicio.

Implementación: validación en el servicio de alta y restricciones en migración,
solo sobre filas no eliminadas.

Hecho cuando: los dos rechazos devuelven un error de dominio a través del
manejador global, no una violación de integridad sin traducir.

Commit: `feat(crm): unicidad de cuentas de canal por empresa y por cuenta externa`

### T-04. Consultas de cuenta sin suposición de unicidad

Tests: con dos cuentas del mismo tipo en la misma empresa, la consulta por empresa
devuelve las dos, y la consulta por identificador de webhook devuelve exactamente
una.

Implementación: métodos nuevos en el repositorio; los que devolvían un único
resultado por empresa y tipo quedan sin uso pero no se borran todavía, para no
romper los llamadores antes del bloque 3.

Hecho cuando: existe un test que registra dos cuentas del mismo tipo y ninguna
consulta falla.

Commit: `refactor(crm): consultas de cuenta de canal por webhook y por empresa`

## Bloque 2 — Identidad y contacto por empresa

### T-05. Empresa en la identidad de canal

Tests: el relleno asigna a cada identidad la empresa de su contacto.

Implementación: columna de empresa en la entidad de identidad y migración con
relleno desde el contacto. Sin cambiar todavía las restricciones.

Hecho cuando: no queda ninguna identidad sin empresa tras la migración.

Commit: `feat(crm): empresa propia en la identidad de canal`

### T-06. Resolución de contacto filtrada por empresa

Tests: RF-22 (la misma identidad externa escribiendo a dos empresas produce dos
contactos independientes) y RF-19 (identidad conocida en la empresa resuelve al
contacto existente). El primero debe fallar contra el código actual: es el defecto
descrito en `spec.md`.

Implementación: la resolución busca por empresa, tipo de canal e identificador
externo en todas sus ramas, incluida la búsqueda por teléfono.

Hecho cuando: el test de fuga entre empresas pasa y ningún test previo se rompe.

Commit: `fix(crm): resolver la identidad de canal dentro de la empresa`

### T-07. Restricciones de unicidad de la identidad de canal

Tests: un contacto admite dos identidades del mismo tipo de canal con
identificadores externos distintos (RF-23).

Implementación: migración que retira las unicidades globales y crea las dos nuevas.
Depende de T-00 para saber qué hacer con las filas en conflicto.

Hecho cuando: la migración aplica sobre la base medida en T-00 y reporta las filas
que no pudo convertir.

Commit: `feat(crm): unicidad de identidades de canal por empresa`

Bloqueada por: T-00.

### T-08. Normalización de teléfono a E.164

Tests: el mismo número en formato local y en formato internacional resuelve al
mismo contacto dentro de la empresa.

Implementación: normalización con país por defecto según lo que se decida.

Hecho cuando: la decisión de origen del país está tomada y documentada en
`spec.md`.

Commit: `fix(crm): normalizar teléfonos a E.164 con país por defecto`

Bloqueada por: [NECESITA ACLARACIÓN: de dónde sale el país por defecto. Ver RF-24.]

## Bloque 3 — Conversación y mensaje anclados a la cuenta

### T-09. Conversación por contacto y cuenta

Tests: RF-26, RF-27 y RF-28. Un contacto que escribe por dos cuentas tiene dos
conversaciones abiertas y sus mensajes no se mezclan. Contra el código actual el
test debe fallar.

Implementación: la búsqueda de conversación abierta incluye la cuenta; consulta
nueva en el repositorio; se conserva la validación de que contacto y cuenta son de
la misma empresa.

Hecho cuando: los tres requisitos están cubiertos y la suite previa sigue verde.

Commit: `fix(crm): abrir una conversación por cada cuenta de canal`

### T-10. Restricción de conversación abierta única por cuenta

Tests: no se pueden crear dos conversaciones abiertas para la misma pareja de
contacto y cuenta.

Implementación: migración con la restricción parcial, precedida del cierre o la
consolidación de las conversaciones que la violen.

Hecho cuando: la migración aplica y el arranque con el perfil `postgres` valida.

Commit: `feat(crm): unicidad de conversación abierta por contacto y cuenta`

Bloqueada por: T-00, T-09.

### T-11. Cuenta de canal en el mensaje

Tests: un mensaje entrante registra la cuenta por la que entró (RF-18).

Implementación: columna en la entidad y migración con relleno desde la
conversación.

Commit: `feat(crm): registrar la cuenta de canal en cada mensaje`

### T-12. Idempotencia respaldada por restricción

Tests: registrar dos veces el mismo mensaje deja una sola fila y no propaga error
(RF-16).

Implementación: restricción única por cuenta e identificador de mensaje del
proveedor; el conflicto se trata como duplicado; se retira la comprobación previa
como único mecanismo. La restricción parcial no existe en H2, así que el test de
suite cubre el tratamiento del conflicto y la restricción se verifica con el
perfil `postgres`.

Hecho cuando: existe constancia de la verificación contra PostgreSQL real, no solo
del test con H2.

Commit: `fix(crm): idempotencia de mensajes entrantes por restricción única`

Bloqueada por: T-00, T-11.

## Bloque 4 — Un único camino de entrada

### T-13. Tipos comunes de evento entrante y de resultado de envío

Tests: no aplican por sí solos; se cubren con los adaptadores.

Implementación: tipos que describen un evento entrante y el resultado de un envío,
sin referencia a ningún proveedor.

Commit: `refactor(crm): tipos comunes de evento de canal`

### T-14. Adaptador con verificación y parseo

Tests: parseo de un lote de Telegram y de un lote de Meta con varios eventos
(RF-14); parseo de mensajes sin texto.

Implementación: la interfaz del adaptador gana verificación y parseo; los dos
adaptadores existentes absorben la lógica que hoy vive en el parser de Telegram y
en el servicio de webhook de Meta.

Hecho cuando: el parseo de un lote con varios eventos devuelve todos.

Commit: `refactor(crm): adaptadores con verificación y parseo propios`

### T-15. Verificación de firma de Meta

Tests: firma válida acepta; firma inválida rechaza; firma ausente rechaza; la
comparación no depende del contenido.

Implementación: HMAC-SHA256 sobre el cuerpo crudo con el secreto de aplicación de
la cuenta, comparación en tiempo constante.

Hecho cuando: el test demuestra que se verifica el cuerpo crudo y no el
deserializado.

Commit: `feat(crm): verificación de firma de los webhooks de Meta`

Bloqueada por: T-02.

### T-16. Registro de eventos de webhook

Tests: un lote recibido dos veces se registra una vez; un evento fallido deja su
motivo registrado.

Implementación: tabla nueva, entidad, repositorio y migración.

Commit: `feat(crm): registro de eventos de webhook`

Bloqueada por: [NECESITA ACLARACIÓN: política de retención del cuerpo crudo.]

### T-17. Controlador y despachador de webhook por cuenta

Tests: RF-11, RF-12, RF-13, RF-15 y RF-17 a nivel de controlador.

Implementación: ruta única por tipo de canal e identificador de webhook, para
verificación y para recepción; despachador que resuelve, verifica, registra,
parsea y procesa cada evento; para Meta, re-resolución por identificador de número
cuando el cuerpo lo trae distinto.

Hecho cuando: una petición con firma inválida no crea contacto, conversación ni
mensaje, comprobado por test.

Commit: `feat(crm): webhook único por cuenta de canal`

Bloqueada por: T-01, T-14, T-15, T-16.

### T-18. Procesador de entrada único

Tests: los dos proveedores producen el mismo resultado de contacto, conversación y
mensaje ante eventos equivalentes; la notificación llega a la empresa
destinataria y solo a ella (RF-31); un fallo de notificación no pierde el mensaje
(RF-32).

Implementación: el procesador recibe la cuenta ya resuelta; el camino de Meta que
vivía en el servicio de mensajes desaparece; se retiran el controlador, los
servicios y los DTO de webhook específicos de proveedor.

Hecho cuando: existe un solo punto de entrada y los tests de ambos proveedores
pasan por él.

Commit: `refactor(crm): un único procesador de mensajes entrantes`

Bloqueada por: T-09, T-12, T-17.

### T-19. Envío por la cuenta de la conversación

Tests: RF-29 con dos cuentas del mismo tipo, la respuesta sale por la correcta;
RF-30, sin identidad registrada el envío se rechaza con motivo.

Implementación: la cuenta y la identidad salen de la conversación; se retira la
creación automática de identidad de WhatsApp a partir del teléfono.

Hecho cuando: la decisión sobre RF-09 y sobre la retirada de la creación
automática está tomada.

Commit: `fix(crm): enviar por la cuenta de canal de la conversación`

Bloqueada por: [NECESITA ACLARACIÓN: RF-09 y el comportamiento del envío sin
mensaje entrante previo.]

## Bloque 5 — Cierre

### T-20. Auto-registro de webhook por cuenta

Tests: la URL construida incluye el identificador de webhook de la cuenta; con dos
cuentas de Telegram se intentan registrar las dos.

Implementación: el registro automático recorre las cuentas de Telegram operativas.

Commit: `feat(crm): auto-registro de webhook para cada cuenta de Telegram`

Bloqueada por: T-17.

### T-21. Retirada de las consultas y rutas obsoletas

Tests: los ya existentes deben seguir pasando sin los elementos retirados.

Implementación: se eliminan las consultas que resuelven por empresa y tipo, y las
rutas de webhook antiguas.

Hecho cuando: una búsqueda en el código no encuentra ninguna referencia a las
piezas retiradas.

Commit: `chore(crm): retirar consultas y rutas de webhook obsoletas`

Bloqueada por: [NECESITA ACLARACIÓN: periodo de compatibilidad de las rutas
antiguas.]

### T-22. Verificación contra PostgreSQL y documentación

Implementación: arranque con el perfil `postgres` sobre base vacía y sobre base
con datos; ejecución de las migraciones en orden; actualización de `AGENTS.md`,
`README.md` y de esta spec con las decisiones tomadas; registro de las acciones
manuales necesarias tras desplegar, que incluyen re-registrar los webhooks en
Telegram y actualizar la URL en el gestor de Meta.

Hecho cuando: existe constancia de la salida real de `./gradlew build`, del
historial de Flyway y de las peticiones de comprobación de los endpoints tocados.

Commit: `docs(crm): verificación y documentación de la spec 001`

## Dependencias resumidas

```
T-00 ──> T-07, T-10, T-12
T-01 ──> T-17
T-02 ──> T-15 ──> T-17
T-03, T-04
T-05 ──> T-06 ──> T-07
T-08 (independiente, bloqueada por decisión)
T-09 ──> T-10, T-18
T-11 ──> T-12 ──> T-18
T-13 ──> T-14 ──> T-17
T-16 ──> T-17 ──> T-18 ──> T-19
T-17 ──> T-20
todo ──> T-21 ──> T-22
```
