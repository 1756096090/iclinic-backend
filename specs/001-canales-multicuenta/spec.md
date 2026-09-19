# 001 — Canales omnicanal multi-cuenta

Estado: borrador, pendiente de revisión.
Ámbito: módulo `crm` (submódulos `channel`, `contact`, `conversation`, `message`,
`webhook`). No entra ningún otro módulo.

Este documento describe el QUÉ y el POR QUÉ. El CÓMO está en `plan.md` y el orden
de ejecución en `tasks.md`.

## 1. Contexto y problema

El CRM ya modela una cuenta de canal como una fila de `crm_channel_connections`,
con su empresa, su sucursal, su tipo de canal, su proveedor y su token cifrado. La
tabla admite varias filas por empresa y tipo. Lo que no lo admite es el código que
la consulta.

Las consultas que resuelven la cuenta a partir de un mensaje entrante devuelven un
único resultado por pareja empresa y tipo de canal
(`ChannelConnectionRepository.findActiveByCompanyAndChannel`, usada en
`ChannelInboundMessageProcessor`, en `MessageServiceImpl.processInboundMessage` y
en la validación del secreto de `TelegramWebhookController`). Con dos números de
WhatsApp registrados para la misma empresa, esa consulta falla o devuelve la
cuenta equivocada, y el mensaje se atribuye a la cuenta que no es.

Alrededor de esa limitación hay cuatro problemas que el mismo trabajo tiene que
resolver, porque comparten las mismas tablas:

1. **Fuga entre empresas en la identidad de canal.** `crm_channel_user_links`
   declara `UNIQUE (channel_type, external_user_id)` de forma global y
   `CrmContactServiceImpl.resolveContact` busca por
   `findByChannelTypeAndExternalUserId` sin filtrar por empresa. Si el mismo
   número de WhatsApp escribe a dos clínicas distintas, la segunda reutiliza el
   contacto de la primera. Es un defecto presente hoy, no solo un obstáculo.
2. **Una conversación abierta por contacto, ignorando el canal.**
   `ConversationServiceImpl.findOrCreateOpenConversation` busca por contacto y
   estado, sin mirar la conexión. Un contacto que escribe por Telegram y por
   WhatsApp acaba con los dos hilos en la misma conversación, y la respuesta sale
   por el canal del hilo original.
3. **La misma persona no queda unificada entre canales.** La restricción
   `UNIQUE (contact_id, channel_type)` impide que un contacto tenga dos
   identidades del mismo tipo de canal, que es justo lo que ocurre cuando una
   empresa tiene dos números de WhatsApp.
4. **Enrutado de webhook acoplado al proveedor.** Telegram recibe el tenant en la
   ruta (`/webhooks/telegram/{companyId}`) y valida el secreto contra la única
   conexión de esa empresa. Meta recibe en una ruta sin token
   (`/webhooks/meta`), acepta cualquier `webhookVerifyToken` que exista en la
   tabla, no verifica la firma `X-Hub-Signature-256` y solo procesa el primer
   mensaje del lote.

Hay además dos caminos distintos para un mensaje entrante
(`ChannelInboundMessageProcessor` para Telegram y
`MessageServiceImpl.processInboundMessage` para Meta) con reglas distintas de
resolución, de idempotencia y de notificación. El segundo descarta la cuenta que
el webhook ya había resuelto y la vuelve a buscar por empresa.

Existe un documento previo, `PLAN.md`, con un análisis coincidente y un diseño de
destino para estas mismas piezas. Esta spec no lo reemplaza: lo reformula como
requisitos verificables y señala dónde falta una decisión de producto.

## 2. Usuarios e historias

**Administrador de la empresa cliente.** Gestiona qué cuentas de canal están
conectadas.

- Como administrador, quiero registrar un segundo número de WhatsApp para mi
  empresa sin pedir un despliegue, para atender sede y urgencias por números
  distintos.
- Como administrador, quiero ver cada cuenta conectada con un nombre que yo
  reconozca y su estado, para saber cuál dejó de recibir mensajes.
- Como administrador, quiero desconectar una cuenta sin perder el historial de
  conversaciones que llegaron por ella.

**Agente de la clínica (recepción o asistente).** Trabaja en la bandeja.

- Como agente, quiero que la respuesta salga por la misma cuenta por la que entró
  el mensaje, para que el paciente vea continuidad.
- Como agente, quiero ver un solo contacto para una persona que me escribe por
  WhatsApp y por Telegram, para no duplicar su ficha.
- Como agente, quiero hilos separados por cuenta, para no mezclar en una misma
  conversación lo que la persona escribió a dos números distintos.

**Paciente.** No usa el sistema; escribe por su canal habitual.

- Como paciente, quiero escribir al número que conozco y recibir la respuesta en
  esa misma conversación.

**Operador de la plataforma.** Mantiene la instalación.

- Como operador, quiero que un reenvío del proveedor no duplique un mensaje en la
  bandeja.
- Como operador, quiero que un webhook no autenticado no pueda inyectar mensajes
  en la cuenta de un cliente.

## 3. Requisitos funcionales

Notación EARS. Cada requisito es verificable de forma independiente.

### Registro y administración de cuentas

- **RF-01.** CUANDO un administrador registre una cuenta de canal para su empresa
  indicando tipo de canal, proveedor, identificador externo de la cuenta y
  credencial de acceso, EL SISTEMA DEBERÁ crear la cuenta asociada a esa empresa y
  devolver su identificador, sin exigir que sea la primera de ese tipo de canal en
  la empresa.
- **RF-02.** EL SISTEMA DEBERÁ admitir un número no acotado de cuentas por pareja
  de empresa y tipo de canal.
  [NECESITA ACLARACIÓN: no hay ninguna cifra en el código ni en la documentación
  que fije un máximo de cuentas por empresa, ni por plan contratado. Si existe un
  límite comercial, debe declararse aquí.]
- **RF-03.** SI ya existe en la misma empresa una cuenta no eliminada con el mismo
  tipo de canal y el mismo identificador externo, ENTONCES EL SISTEMA DEBERÁ
  rechazar el registro e indicar cuál es la cuenta existente.
- **RF-04.** SI el identificador externo de la cuenta ya está registrado en otra
  empresa, ENTONCES EL SISTEMA DEBERÁ rechazar el registro, porque una cuenta
  externa pertenece a un único tenant.
- **RF-05.** CUANDO se registre una cuenta, EL SISTEMA DEBERÁ generar para ella un
  identificador de webhook propio, distinto del identificador de la empresa, y
  exponer al administrador la URL pública de webhook que corresponde a esa cuenta.
- **RF-06.** CUANDO el administrador consulte las cuentas de canal, EL SISTEMA
  DEBERÁ devolver únicamente las de su propia empresa.
- **RF-07.** EL SISTEMA DEBERÁ almacenar cifrada toda credencial de una cuenta de
  canal y no devolverla nunca en una respuesta de la API.
- **RF-08.** CUANDO un administrador desactive una cuenta, EL SISTEMA DEBERÁ dejar
  de aceptar mensajes entrantes por ella y conservar sus conversaciones y mensajes
  históricos.
- **RF-09.** SI un administrador intenta enviar un mensaje por una conversación
  cuya cuenta está desactivada o eliminada, ENTONCES EL SISTEMA DEBERÁ rechazar el
  envío e indicar el motivo.
  [NECESITA ACLARACIÓN: alternativa posible es reasignar la conversación a otra
  cuenta activa del mismo tipo. Es una decisión de producto, no técnica.]
- **RF-10.** CUANDO un administrador dé de alta una cuenta de un tipo de canal
  para el que no exista adaptador registrado, EL SISTEMA DEBERÁ rechazar el alta e
  indicar los tipos admitidos.

### Recepción de mensajes

- **RF-11.** CUANDO llegue una petición a la URL de webhook de una cuenta, EL
  SISTEMA DEBERÁ resolver la cuenta destinataria a partir del identificador de
  webhook de la URL y, para los proveedores que lo incluyan, del identificador de
  cuenta presente en el cuerpo, sin tomar nunca la empresa de un parámetro
  suministrado por quien llama.
- **RF-12.** SI la petición de webhook no supera la verificación de autenticidad
  del proveedor (firma criptográfica o secreto compartido, según el proveedor),
  ENTONCES EL SISTEMA DEBERÁ rechazarla sin crear contacto, conversación ni
  mensaje.
- **RF-13.** SI el identificador de webhook de la URL no corresponde a ninguna
  cuenta operativa, ENTONCES EL SISTEMA DEBERÁ rechazar la petición sin revelar si
  la cuenta existe pero está inactiva.
  [NECESITA ACLARACIÓN: distinguir entre no existe e inactiva facilita el
  diagnóstico al administrador. Hay que elegir entre discreción y diagnóstico.]
- **RF-14.** CUANDO el cuerpo de un webhook contenga varios eventos, EL SISTEMA
  DEBERÁ procesar todos, y no solo el primero.
- **RF-15.** SI el procesamiento de un evento individual falla, ENTONCES EL
  SISTEMA DEBERÁ procesar el resto de eventos del mismo lote y dejar constancia
  del fallo.
- **RF-16.** CUANDO EL SISTEMA reciba dos veces el mismo mensaje del proveedor,
  identificado por la cuenta y el identificador de mensaje del proveedor, DEBERÁ
  registrarlo una sola vez, incluso si las dos entregas se procesan
  simultáneamente.
- **RF-17.** CUANDO EL SISTEMA haya verificado la autenticidad de la petición,
  DEBERÁ responder al proveedor de forma que este no reintente la entrega, aunque
  algún evento del lote haya fallado.
- **RF-18.** CUANDO se registre un mensaje entrante, EL SISTEMA DEBERÁ dejar
  constancia de por qué cuenta de canal entró.

### Identidad y unificación del contacto

- **RF-19.** CUANDO llegue un mensaje de una identidad externa ya conocida en esa
  empresa, EL SISTEMA DEBERÁ asociarlo al contacto existente, con independencia de
  por cuál de las cuentas de esa empresa haya entrado.
- **RF-20.** CUANDO llegue un mensaje de una identidad externa desconocida cuyo
  teléfono normalizado coincida con el de un contacto de la misma empresa, EL
  SISTEMA DEBERÁ asociarlo a ese contacto y registrar la identidad nueva bajo él.
- **RF-21.** CUANDO llegue un mensaje de una identidad externa desconocida sin
  coincidencia de teléfono en la empresa, EL SISTEMA DEBERÁ crear un contacto
  nuevo y registrar la identidad bajo él.
- **RF-22.** SI la misma identidad externa escribe a cuentas de dos empresas
  distintas, ENTONCES EL SISTEMA DEBERÁ mantener un contacto independiente en cada
  empresa, sin compartir datos entre ellas.
- **RF-23.** EL SISTEMA DEBERÁ permitir que un mismo contacto tenga varias
  identidades del mismo tipo de canal.
- **RF-24.** EL SISTEMA DEBERÁ normalizar todo teléfono a formato E.164 antes de
  usarlo para comparar o deduplicar.
  [NECESITA ACLARACIÓN: la normalización actual antepone un signo más al número
  sin prefijo, sin conocer el país. Un número local de Ecuador y el mismo número
  en formato internacional no se reconocen como el mismo. Hace falta decidir de
  dónde sale el país por defecto: de la empresa, de la sucursal o de la cuenta de
  canal.]
- **RF-25.** CUANDO el proveedor informe un nombre visible o un alias distinto del
  registrado para una identidad conocida, EL SISTEMA DEBERÁ actualizar esos datos
  en la identidad y no crear un contacto nuevo.
  [NECESITA ACLARACIÓN: no está definido si el nombre del contacto debe seguir al
  del canal, o si prevalece el que haya escrito el personal de la clínica.]

### Conversaciones y envío

- **RF-26.** CUANDO llegue un mensaje de un contacto por una cuenta concreta y ese
  contacto tenga una conversación abierta en esa misma cuenta, EL SISTEMA DEBERÁ
  añadir el mensaje a esa conversación.
- **RF-27.** CUANDO llegue un mensaje de un contacto por una cuenta en la que no
  tenga conversación abierta, EL SISTEMA DEBERÁ abrir una conversación nueva para
  esa pareja de contacto y cuenta, aunque el contacto tenga conversaciones
  abiertas en otras cuentas.
- **RF-28.** MIENTRAS un contacto tenga conversaciones abiertas en varias cuentas,
  EL SISTEMA DEBERÁ mantenerlas separadas y no mezclar sus mensajes.
- **RF-29.** CUANDO un agente responda en una conversación, EL SISTEMA DEBERÁ
  enviar el mensaje por la cuenta de canal de esa conversación y a la identidad
  externa por la que llegó el hilo.
- **RF-30.** SI la conversación no tiene una identidad externa registrada para su
  tipo de canal, ENTONCES EL SISTEMA DEBERÁ rechazar el envío e indicar que hace
  falta un mensaje entrante previo del contacto.
- **RF-31.** CUANDO se registre un mensaje entrante, EL SISTEMA DEBERÁ notificar a
  la bandeja de la empresa destinataria, y solo a ella, indicando la cuenta de
  canal por la que llegó.
- **RF-32.** SI la notificación en tiempo real falla, ENTONCES EL SISTEMA DEBERÁ
  conservar el mensaje registrado y no perder la recepción.

## 4. Casos límite y de error

| Caso | Comportamiento esperado |
|---|---|
| Dos cuentas de WhatsApp en la misma empresa y llega un mensaje a una de ellas | El mensaje se atribuye a la cuenta destinataria; la otra no se ve afectada |
| La misma persona escribe a los dos números de WhatsApp de la misma empresa | Un solo contacto, dos identidades, dos conversaciones |
| La misma persona escribe a la clínica A y a la clínica B | Dos contactos independientes, sin datos compartidos |
| Meta reenvía un lote ya procesado | Ningún mensaje duplicado; sin error hacia el proveedor |
| Dos entregas simultáneas del mismo mensaje | Una sola fila registrada; la segunda se descarta sin fallo visible |
| Lote con varios eventos donde uno está malformado | Los demás se procesan; el fallido queda registrado con su motivo |
| Webhook con firma ausente o inválida | Rechazo; ningún dato creado |
| Webhook con identificador de webhook inexistente | Rechazo |
| Cuenta desactivada que sigue recibiendo del proveedor | Se rechaza la recepción; queda constancia |
| Mensaje entrante sin texto (imagen, ubicación, sticker, contacto) | Se registra con su tipo; la bandeja muestra un resumen adecuado |
| Telegram no informa teléfono | El contacto se crea sin teléfono; la unificación depende de la identidad de canal |
| Teléfono que llega con formato local en un canal e internacional en otro | [NECESITA ACLARACIÓN: depende de la decisión de RF-24; hoy resultarían dos contactos] |
| Credencial de la cuenta caducada o revocada por el proveedor | [NECESITA ACLARACIÓN: no hay definido un estado de cuenta en fallo ni un aviso al administrador] |
| Agente responde a una conversación cuya cuenta se desactivó entretanto | Según RF-09, pendiente de decisión |
| Contacto que ya quedó fusionado entre empresas por el defecto actual | [NECESITA ACLARACIÓN: hay que decidir si la migración los separa automáticamente, los deja como están y los reporta, o exige limpieza manual antes de migrar] |
| Dos identidades de canal distintas que resultan ser la misma persona | [NECESITA ACLARACIÓN: no existe operación de fusión de contactos y no está pedida] |

## 5. Fuera de alcance

- Campañas, plantillas de mensaje y envíos masivos.
- Canales nuevos: web chat, Instagram y Facebook como canales operativos. Los dos
  últimos ya existen como valores del enum, pero sin adaptador.
  [NECESITA ACLARACIÓN: confirmar que quedan fuera de esta spec.]
- Aislamiento transversal automático por tenant (contexto de tenant y filtro a
  nivel de ORM). Esta spec exige el filtrado explícito por empresa en los caminos
  que toca, no un mecanismo general.
- Catálogo de módulos contratables por empresa.
- Fusión manual de contactos y edición de identidades desde la API.
- Recibos de entrega y de lectura, y estados de mensaje más allá de los actuales.
- Cualquier cambio en `appointment`, `user`, `branch`, `company` y `admin`.
- Interfaz de usuario de la bandeja.

## 6. Criterios de aceptación

Cada criterio debe poder comprobarse con un test automatizado o con una secuencia
de peticiones reproducible.

1. Una empresa con dos cuentas de WhatsApp registradas recibe un mensaje en cada
   una y cada mensaje queda asociado a su cuenta.
2. Registrar una segunda cuenta de Telegram en la misma empresa no provoca error
   en la recepción de ninguna de las dos.
3. La misma identidad externa escribiendo a dos empresas distintas produce dos
   contactos, y la consulta de contactos de cada empresa devuelve solo el suyo.
4. Una persona que escribe por Telegram y por WhatsApp a la misma empresa, con el
   mismo teléfono conocido, queda como un único contacto con dos identidades.
5. Esa misma persona tiene dos conversaciones abiertas, una por cuenta, y los
   mensajes no se mezclan.
6. La respuesta de un agente sale por la cuenta de la conversación; con dos
   cuentas del mismo tipo, sale por la correcta.
7. Reenviar el mismo cuerpo de webhook dos veces deja una sola fila de mensaje.
   Enviarlo dos veces en paralelo también.
8. Un webhook con firma inválida se rechaza y no crea contacto, conversación ni
   mensaje.
9. Un lote con varios mensajes registra todos.
10. Ningún endpoint del módulo CRM devuelve datos de una empresa distinta a la del
    usuario autenticado.
11. Ninguna respuesta de la API contiene una credencial de canal.
12. La aplicación arranca con el perfil `postgres` sobre una base vacía, Flyway
    aplica en orden y `ddl-auto=validate` pasa.
13. `./gradlew build` pasa completo, con la suite previa incluida.
14. Los endpoints ajenos al CRM responden con la misma ruta, método y forma de
    respuesta que antes del cambio.

## 7. Dudas abiertas

Recogidas aquí las que no bloquean la redacción del plan pero sí condicionan
detalles de la implementación.

1. Estrategia de migración de los datos existentes en `crm_channel_user_links` y
   `crm_conversations`, en particular los contactos que el defecto actual pudo
   fusionar entre empresas.
2. Política de nombre del contacto cuando el canal informa uno distinto del
   guardado.
3. País por defecto para normalizar teléfonos.
4. Qué hacer con una conversación cuya cuenta deja de estar operativa.
5. Si el estado de una cuenta debe reflejar el fallo de credencial detectado al
   enviar, y cómo se avisa al administrador.
6. Si el ciclo de estados actual (pendiente, verificada, activa, inactiva) sigue
   teniendo sentido con verificación por cuenta, o se simplifica.
7. Compatibilidad hacia atrás de las rutas de webhook actuales: si deben seguir
   funcionando durante un tiempo o se cortan de golpe con re-registro manual en
   cada proveedor.
8. Si `INSTAGRAM` y `FACEBOOK` deben quedar rechazados explícitamente al registrar
   una cuenta mientras no tengan adaptador.
