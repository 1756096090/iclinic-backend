# Constitución del proyecto

Principios estables de iClinic Backend. No contiene tareas ni plazos: lo que
cambia con cada funcionalidad va en `specs/`. Un documento de `specs/` que
contradiga esta constitución está mal, salvo que la constitución se modifique
antes y de forma explícita.

## 1. Objetivos del producto

1. Dar a una clínica dental una agenda de citas y una bandeja de conversaciones
   con sus pacientes en el mismo sistema, de modo que un mensaje entrante y una
   cita hablen del mismo contacto.
2. Atender a varias empresas cliente sobre una única instalación, con separación
   estricta de sus datos.
3. Operar sobre varios países (Ecuador, Colombia, Perú) con las diferencias
   fiscales y de identificación modeladas en el dominio.
4. Ser omnicanal: el personal de la clínica trabaja en una sola bandeja,
   independientemente del canal por el que llegó cada mensaje.
5. Permitir que una empresa conecte sus propias cuentas de canal sin que eso
   exija un cambio de código ni un despliegue.

[NECESITA ACLARACIÓN: no hay en el repositorio ninguna constancia de objetivos de
negocio cuantificados (número de empresas a soportar, volumen de mensajes, SLA de
entrega). Todos los objetivos anteriores están inferidos del código y del README.]

## 2. Stack y tecnologías

Permitidas, porque ya están en uso y sostienen el diseño actual:

| Pieza | Elección |
|---|---|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.5.13-SNAPSHOT (web, data-jpa, validation, security, websocket) |
| Construcción | Gradle |
| Base de datos de verdad | PostgreSQL 16 |
| Migraciones | Flyway (`flyway-core` + `flyway-database-postgresql`) |
| Base de datos de conveniencia | H2, solo para arranque local sin Docker y para la suite de tests |
| Autenticación | Firebase Admin SDK, tokens de ID verificados en un filtro |
| Documentación de API | springdoc-openapi |
| Reducción de repetición | Lombok |
| Cliente HTTP saliente | `RestTemplate` compartido |
| Tiempo real hacia el frontend | WebSocket/STOMP y SSE |

Prohibidas mientras no se apruebe lo contrario por escrito:

- Brokers de mensajería o colas externas (RabbitMQ, Kafka), cachés distribuidas
  (Redis) y planificadores externos (Quartz). El trabajo diferido se resuelve con
  `@Scheduled` y una tabla de outbox en la propia base.
- ORMs, frameworks web o de inyección alternativos al stack anterior.
- Generación de esquema por Hibernate en cualquier entorno que no sea `h2` o
  `test`.
- Lógica de negocio escrita en SQL de migración. Las migraciones mueven
  estructura y datos, no reglas.
- Dependencias que obliguen a tener Docker levantado para ejecutar `./gradlew test`.
- Llamadas a servicios de terceros no declarados en esta tabla.

## 3. Buenas prácticas obligatorias

### 3.1 Capas y organización

El código se organiza por módulo funcional, y dentro de cada módulo por capa:
`controller` hacia `service` hacia `repository` hacia `entity`. Un controlador no
accede a un repositorio. Una entidad no sale del `service` hacia el exterior:
cruza siempre por un `mapper` hacia un DTO. El código compartido entre módulos
vive en `shared/` o en `config/`; un módulo no depende de las clases internas de
otro más allá de su entidad y su servicio públicos.

La variación de comportamiento por tipo (país, tipo de sucursal, proveedor de
canal) se expresa con herencia de entidades y con Strategy/Registry/Factory, no
con condicionales sobre un enum repartidos por los servicios. Añadir un país o un
proveedor debe ser añadir una clase.

### 3.2 Aislamiento multi-tenant

`Company` es la frontera del tenant. Toda entidad con datos de cliente pertenece a
una empresa, directamente o a través de otra entidad.

- Todo acceso a datos se filtra por empresa. Una consulta por identificador
  externo (teléfono, identificador de usuario de un canal, identificador de
  mensaje del proveedor) que no incluya la empresa es un defecto.
- El tenant se deduce del usuario autenticado o de la conexión de canal
  registrada. No se acepta desde un parámetro de ruta, una cabecera o el cuerpo
  de un webhook.
- Las claves de unicidad sobre identificadores de terceros se definen por empresa,
  no globalmente, salvo cuando el identificador designa una cuenta propia de la
  plataforma y debe pertenecer a un único tenant.
- Los endpoints que listan sin filtro de empresa son un defecto, no una comodidad
  de desarrollo.

### 3.3 Migraciones versionadas

La estructura de la base vive en `db/migration/`, en orden, sin editar lo ya
aplicado. Cada migración se escribe para PostgreSQL, es reproducible sobre una
base vacía y, cuando mueve datos, deja constancia de las filas que no pudo
convertir en lugar de descartarlas en silencio. `ddl-auto=validate` en `postgres`
es el control que prueba que las entidades y las migraciones no divergieron.

### 3.4 Credenciales y secretos

Los secretos de terceros se guardan cifrados con AES-256/GCM a través de
`SecretEncryptionService`, con la clave inyectada por entorno. Un secreto no
aparece en claro en el repositorio, en los logs ni en una respuesta de la API. Los
caminos de compatibilidad que permiten guardar o leer un secreto en claro son
deuda a eliminar, no una característica.

### 3.5 Tests

Cada comportamiento nuevo llega con su test, escrito antes que la implementación.
La suite corre sin Docker. Un test que necesita PostgreSQL de verdad se marca como
condicional y no bloquea `./gradlew test`. Los casos de aislamiento entre empresas
y los de idempotencia de mensajes se prueban explícitamente: son las dos familias
de fallo con consecuencias visibles para un cliente.

### 3.6 Integraciones entrantes

Un webhook público verifica la autenticidad de quien llama (firma criptográfica o
secreto compartido) antes de hacer nada con el contenido. El procesamiento de un
evento es idempotente y está respaldado por una restricción de unicidad en la
base, no solo por una comprobación previa en memoria. El sistema responde al
proveedor de forma que no provoque reintentos indefinidos por un fallo propio.

## 4. Alcance del producto

Dentro del alcance:

- Gestión de empresas, sucursales, usuarios y roles del personal de clínica.
- Agenda de citas: disponibilidad por doctor y sucursal, creación, reprogramación
  y cancelación.
- CRM omnicanal: cuentas de canal por empresa, contactos unificados,
  conversaciones, mensajes entrantes y salientes, notificación en tiempo real a la
  bandeja.
- Autenticación federada con Firebase y autorización por rol.

Fuera del alcance:

- Historia clínica, odontograma, recetas y cualquier dato clínico más allá de la
  cita.
- Facturación, cobros, pasarelas de pago y emisión de comprobantes fiscales.
- Interfaz de usuario. Este repositorio es exclusivamente la API.
- Inventario, nómina, contabilidad.
- Envío de correo electrónico o SMS como canales del CRM.

[NECESITA ACLARACIÓN: el alcance "fuera" está deducido de la ausencia de esas
entidades en el código. Conviene confirmar cuáles son decisiones firmes y cuáles
son simplemente cosas que aún no se han construido.]

## 5. Criterios de calidad: qué significa "terminado"

Una unidad de trabajo está terminada cuando se cumplen todas estas condiciones:

1. `./gradlew build` compila y la suite completa pasa, con el número de tests y de
   fallos reportado a partir de la salida real.
2. Existen tests que cubren el comportamiento nuevo, incluidos sus casos de error,
   y fueron escritos antes que la implementación.
3. Si hubo cambios de estructura, la aplicación arranca con el perfil `postgres`
   sobre una base vacía, Flyway aplica en orden y `ddl-auto=validate` pasa.
4. Los endpoints preexistentes siguen respondiendo con la misma ruta, método y
   forma de respuesta; cualquier cambio de contrato está declarado por escrito con
   su ruta anterior y su sustituta.
5. Ninguna consulta introducida devuelve datos sin filtrar por empresa.
6. No hay secretos en claro, ni en código ni en datos de ejemplo.
7. La documentación afectada (`AGENTS.md`, `README.md`, el documento de la spec)
   queda al día en el mismo cambio.
8. Lo que quedó fuera está dicho explícitamente, con su motivo.

Lo que no cuenta como terminado: que compile, que funcione en la máquina de quien
lo escribió, que los tests estén desactivados, o un informe de avance en
porcentaje.
