# 001 — Plan técnico

Borrador. Cubre el CÓMO de `spec.md`. No contiene código; describe piezas,
responsabilidades y contratos. El orden de ejecución está en `tasks.md`.

Este plan es consistente con el diseño de las fases 1 y 2 de `PLAN.md`, que ya
contiene un análisis del mismo problema. Donde difiere, se señala.

## 1. Enfoque general

El esquema actual ya admite varias cuentas por empresa y tipo de canal. El cambio
es de código, no de concepto: se sustituyen las resoluciones que asumen una única
cuenta por resoluciones dirigidas a una cuenta concreta, identificada por el
propio webhook.

Cuatro movimientos, en este orden:

1. **La cuenta se resuelve por su propio identificador, no por la empresa.** Cada
   cuenta de canal recibe un identificador de webhook único. La URL pública del
   webhook lleva ese identificador. La empresa se deduce de la cuenta encontrada.
   Ninguna consulta de recepción vuelve a partir de la pareja empresa y tipo.
2. **La identidad de canal se ancla a la empresa.** Las restricciones de unicidad
   globales sobre el identificador externo pasan a ser por empresa, y la
   resolución del contacto filtra siempre por empresa. Esto cierra la fuga entre
   tenants y, de paso, permite que un contacto tenga varias identidades del mismo
   tipo de canal.
3. **La conversación se ancla a la cuenta.** La búsqueda de conversación abierta
   pasa a ser por la pareja contacto y cuenta, respaldada por una restricción de
   unicidad parcial.
4. **Un único camino de entrada.** Los dos caminos actuales convergen en uno:
   verificar autenticidad, registrar el evento, parsear a un tipo común, procesar
   cada evento. El proveedor solo aporta la verificación y el parseo.

Se conserva el patrón Registry por autodescubrimiento: añadir un proveedor sigue
siendo añadir una clase que implemente el adaptador.

## 2. Componentes afectados

### Se modifica

| Pieza | Cambio |
|---|---|
| `crm/channel/entity/ChannelConnection` | Campos nuevos: nombre visible, identificador de webhook único, secreto de aplicación cifrado, identificador de cuenta de negocio. Pasa a extender `BaseEntity` para tener marca de actualización |
| `crm/channel/repository/ChannelConnectionRepository` | Los métodos de resolución devuelven listas o buscan por identificador de webhook; desaparecen los que devuelven un único resultado por empresa y tipo |
| `crm/channel/service` y `mapper` y `dto` | Alta con validación de unicidad por empresa y global; respuesta con nombre visible y URL de webhook; nunca con credenciales |
| `crm/channel/adapter/MessagingChannelAdapter` | Interfaz ampliada: además de enviar, verifica la autenticidad de una petición entrante y convierte el cuerpo del proveedor en eventos comunes |
| `TelegramAdapter`, `WhatsAppMetaAdapter` | Absorben el parseo que hoy vive en `TelegramUpdateParser` y `MetaWebhookServiceImpl`, y añaden verificación |
| `crm/contact/entity/ChannelUserLink` (identidad de canal) | Gana empresa propia; cambian sus restricciones de unicidad |
| `crm/contact/service/CrmContactServiceImpl` | `resolveContact` filtra por empresa en todas sus ramas |
| `crm/conversation/service/ConversationServiceImpl` | `findOrCreateOpenConversation` y `hasOpenConversation` toman la cuenta en cuenta |
| `crm/conversation/repository/ConversationRepository` | Consulta por contacto, cuenta y estado |
| `crm/message/entity/CrmMessage` | Gana la cuenta de canal por la que entró o salió |
| `crm/message/service/MessageServiceImpl` | El envío resuelve la identidad por cuenta; deja de re-buscar la cuenta por empresa; `processInboundMessage` desaparece en favor del camino único |
| `crm/webhook/service/ChannelInboundMessageProcessor` | Recibe la cuenta ya resuelta; la idempotencia se apoya en la restricción de unicidad |
| `config/TelegramWebhookAutoRegister`, `TelegramWebhookBaseUrlResolver` | Construyen la URL con el identificador de webhook de cada cuenta, y registran todas las cuentas de Telegram, no una |
| `config/SecurityConfig` | Las rutas de webhook siguen públicas; la nueva forma de ruta entra en el mismo patrón |

### Se crea

| Pieza | Responsabilidad |
|---|---|
| Controlador único de webhook | Una ruta por tipo de canal e identificador de webhook, para verificación y para recepción |
| Despachador de webhook | Orquesta: resolver cuenta, verificar, registrar evento, parsear, procesar cada evento, responder |
| Tipos comunes de evento entrante y de resultado de envío | Desacoplan el procesador del proveedor |
| Verificador de firma de Meta | HMAC-SHA256 con el secreto de aplicación, comparación en tiempo constante |
| Registro de eventos de webhook | Auditoría y segunda línea de idempotencia, por cuenta e identificador de evento del proveedor |
| Excepciones de dominio nuevas | Cuenta no encontrada, cuenta duplicada, firma inválida |

### No se toca

`appointment`, `user`, `branch`, `company`, `admin`, `auth`, `access`. Tampoco se
introduce contexto de tenant transversal ni filtro a nivel de ORM: queda para un
trabajo posterior y esta spec exige el filtrado explícito.

## 3. Modelo de datos

Cambios sobre el esquema de `V1__baseline_schema.sql`. Todos por migración Flyway,
ninguno por generación de Hibernate.

### `crm_channel_connections`

Columnas nuevas: nombre visible para el administrador, identificador de webhook
(único, generado al crear), secreto de aplicación cifrado, identificador de cuenta
de negocio del proveedor, marca de actualización.

Restricciones nuevas, ambas solo sobre filas no eliminadas:

- única por empresa, tipo de canal e identificador externo de cuenta;
- única por tipo de canal e identificador externo de cuenta, que es lo que impide
  que la misma cuenta externa quede registrada en dos empresas (RF-04).

Índice nuevo por identificador de webhook, que es la entrada de toda recepción.

El identificador de webhook debe generarse también para las filas existentes en la
propia migración.

[NECESITA ACLARACIÓN: `PLAN.md` propone además renombrar la tabla a
`crm_channel_accounts` y las rutas a `/api/v1/crm/channel-accounts`. El renombrado
no es necesario para la funcionalidad de esta spec y arrastra un cambio de
contrato. Hay que decidir si entra aquí o se difiere.]

### Identidad de canal (`crm_channel_user_links`)

Columna nueva: empresa, con relleno desde la empresa del contacto.

Restricciones: se retira la unicidad global por tipo de canal e identificador
externo, y se retira la unicidad por contacto y tipo de canal. Entran:

- única por empresa, tipo de canal e identificador externo (RF-19, RF-22);
- única por contacto, tipo de canal e identificador externo (RF-23).

La unicidad es por empresa y no por cuenta, deliberadamente: si una clínica tiene
dos números de WhatsApp y la misma persona escribe a los dos, sigue siendo una
sola persona. Es lo que hace posible RF-19.

Riesgo de datos: si hoy existen filas de empresas distintas que comparten
identificador externo, el relleno las separa correctamente porque la empresa viene
del contacto. Si un contacto quedó fusionado entre empresas por el defecto actual,
la migración no puede deshacerlo sin criterio de negocio.
[NECESITA ACLARACIÓN: decidir entre reportar y dejar como está, separar
automáticamente, o exigir limpieza previa. Es la dependencia principal de
`tasks.md`.]

### `crm_conversations`

Restricción nueva: única por contacto y cuenta de canal, solo sobre conversaciones
abiertas (RF-27, RF-28). Columna nueva opcional que registra por qué identidad
concreta llegó el hilo, necesaria cuando un contacto tiene varias identidades del
mismo tipo de canal (RF-29).

Antes de crear la restricción hay que consolidar los datos existentes: hoy puede
haber varias conversaciones abiertas del mismo contacto en cuentas distintas, y
también conversaciones abiertas que la restricción declararía duplicadas.
[NECESITA ACLARACIÓN: qué hacer con las conversaciones abiertas que colisionen.]

### `crm_messages`

Columna nueva: cuenta de canal (RF-18), rellenada desde la conversación.
Restricción única nueva por cuenta e identificador de mensaje del proveedor, solo
cuando ese identificador no es nulo. Esta restricción es lo que convierte la
idempotencia de RF-16 en una garantía real y no en una comprobación previa con
carrera.

Antes de crearla hay que detectar y resolver duplicados existentes.

### Tabla nueva de eventos de webhook

Cuenta, proveedor, identificador de evento del proveedor, cuerpo recibido,
resultado de la verificación, momento de recepción, momento de procesamiento y
error. Única por cuenta e identificador de evento.

[NECESITA ACLARACIÓN: guardar el cuerpo crudo implica retener datos personales de
pacientes. Falta una política de retención y de purga.]

## 4. Dependencias entre piezas

```
Migración de cuentas (identificador de webhook, restricciones)
        |
        +--> Controlador y despachador de webhook por cuenta
        |            |
        |            +--> Adaptadores con verificación y parseo
        |                          |
        |                          +--> Verificador de firma de Meta
        |
Migración de identidades (empresa, unicidad por empresa)
        |
        +--> Resolución de contacto filtrada por empresa
                     |
Migración de conversaciones (unicidad por contacto y cuenta)
        |            |
        +------------+--> Conversación por cuenta
                                  |
Migración de mensajes (cuenta, unicidad de idempotencia)
        |                         |
        +-------------------------+--> Procesador único de entrada
                                                |
                                                +--> Envío por la cuenta de la conversación
                                                |
                                                +--> Auto-registro de webhook por cuenta
```

Reglas de orden que se derivan:

- Ninguna restricción de unicidad se crea antes de limpiar los datos que la
  violarían.
- El controlador nuevo no puede existir sin el identificador de webhook.
- El procesador único no puede converger antes de que la conversación esté anclada
  a la cuenta, porque hoy los dos caminos difieren precisamente ahí.
- El auto-registro de Telegram es lo último: depende de la URL definitiva.

## 5. Enfoque técnico, decisión por decisión

**Resolución de la cuenta entrante.** El identificador de webhook de la URL es la
fuente primaria. Para Meta, una misma aplicación sirve a varios números, así que si
el cuerpo trae un identificador de número distinto, se vuelve a resolver la cuenta
por ese identificador, siempre contra la base y nunca confiando en el valor como
prueba de pertenencia.

**Verificación de autenticidad.** Telegram: secreto compartido en cabecera,
comparado contra el de esa cuenta. Meta: HMAC-SHA256 del cuerpo crudo con el
secreto de aplicación de esa cuenta. El secreto de aplicación no existe hoy en
ninguna parte del modelo; es el campo nuevo que hace posible RF-12 para Meta. El
cuerpo debe verificarse tal como llegó, antes de cualquier deserialización.

**Idempotencia.** Doble línea: el registro de eventos de webhook descarta un lote
ya recibido; la restricción única sobre mensaje descarta un mensaje ya registrado.
La colisión de la restricción se trata como duplicado, no como error. Se retira la
comprobación previa como único mecanismo.

**Respuesta al proveedor.** Verificación válida implica respuesta de aceptación,
aunque un evento del lote falle; el fallo queda en el registro de eventos (RF-15,
RF-17). Verificación inválida implica rechazo.

**Unificación del contacto.** Orden de resolución, siempre dentro de la empresa de
la cuenta: identidad de canal conocida, luego teléfono normalizado, luego contacto
nuevo. Es el orden actual, con el filtro por empresa añadido.

**Normalización de teléfono.** La actual antepone un signo más sin conocer el
país, lo que no produce E.164 para números locales. Hace falta un origen de país.
[NECESITA ACLARACIÓN: empresa, sucursal o cuenta de canal como origen del prefijo
por defecto. Afecta a RF-20 y RF-24.]

**Envío.** La cuenta sale de la conversación, no de una búsqueda por empresa. La
identidad destino sale de la conversación cuando está registrada; si no, de la
identidad del contacto para esa cuenta. Se retira la creación automática de
identidad de WhatsApp a partir del teléfono del contacto: con varias cuentas deja
de ser determinista.
[NECESITA ACLARACIÓN: hoy esa creación automática permite iniciar conversación
saliente sin mensaje entrante previo. Retirarla cambia un comportamiento
existente.]

**Adaptador.** Pasa a exponer tipo soportado, verificación, parseo a eventos
comunes y envío. El registro sigue autodescubriendo implementaciones.

**Compatibilidad de rutas.** Las rutas de webhook actuales quedan obsoletas. El
corte exige re-registrar el webhook de cada bot de Telegram y actualizar la URL de
devolución en el gestor de Meta.
[NECESITA ACLARACIÓN: si debe existir un periodo en el que las rutas antiguas
sigan funcionando, y de cuánto.]

**Tests.** Todo con H2 en el perfil `test`. Las restricciones únicas parciales no
existen en H2: los casos que dependen de ellas se prueban al nivel de servicio con
comportamiento simulado, y la restricción real se verifica arrancando con el
perfil `postgres`. No se introduce Testcontainers.

## 6. Criterios de aceptación técnicos

1. No queda ninguna consulta de recepción que resuelva la cuenta a partir de la
   pareja empresa y tipo de canal.
2. Ninguna consulta de resolución de contacto o de identidad de canal carece de
   filtro por empresa.
3. Existe un único punto de entrada para mensajes entrantes, común a los dos
   proveedores.
4. Las restricciones de unicidad descritas existen en migración y `ddl-auto=validate`
   pasa con el perfil `postgres` sobre una base vacía.
5. Las migraciones aplican en orden sobre una base con los datos de
   `db/seed/dev-seed.sql` cargados, y las filas que no se pueden convertir quedan
   reportadas en la salida de la migración.
6. Ninguna respuesta de la API expone credenciales ni secretos de canal.
7. La verificación de firma se ejecuta sobre el cuerpo crudo y antes de cualquier
   deserialización.
8. La suite completa pasa sin Docker. Los tests que exigen PostgreSQL están
   marcados como condicionales.
9. Los adaptadores nuevos se registran solos: no hay ningún condicional sobre tipo
   de canal fuera de los adaptadores y sus fábricas.
10. `./gradlew build` pasa, y los endpoints de módulos ajenos al CRM responden
    igual que antes.

## 7. Riesgos

| Riesgo | Naturaleza |
|---|---|
| Datos existentes que violan las restricciones nuevas | Bloquea la migración; hay que medirlo antes de escribirla |
| Contactos fusionados entre empresas por el defecto actual | No se puede deshacer sin criterio de negocio |
| Corte de las rutas de webhook | Requiere acción manual en Telegram y en el gestor de Meta; hay ventana de mensajes perdidos |
| Retirada de la creación automática de identidad de WhatsApp | Cambia un comportamiento existente del envío |
| H2 no reproduce las restricciones únicas parciales | La suite no puede probar la garantía real de idempotencia |
| Retención del cuerpo crudo de los webhooks | Datos personales sin política de purga definida |
