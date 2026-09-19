# AGENTS.md

Reglas operativas para agentes que trabajan en este repositorio. Es el único
fichero de instrucciones; `CLAUDE.md` solo lo referencia.

## Qué es este proyecto

iClinic Backend es una API REST en Spring Boot para una plataforma de clínicas
dentales multi-empresa y multi-país (Ecuador, Colombia, Perú). Combina agenda de
citas con un CRM omnicanal que recibe y envía mensajes por Telegram y por la API
de Meta (WhatsApp). Cada empresa (`Company`) es un tenant: sus contactos,
conversaciones, canales y citas no deben cruzarse con los de otra empresa.

## Comandos

```bash
./gradlew build                 # compila + ejecuta toda la suite
./gradlew test                  # solo tests
./gradlew clean build
./gradlew generateSchema        # vuelca el DDL PostgreSQL de las entidades a build/generated-schema.sql
./gradlew test --tests "com.iclinic.iclinicbackend.modules.crm.channel.service.ChannelConnectionServiceTest"
./gradlew test --tests "*MessageServiceImplTest.metodoConcreto"
```

Arranque:

```bash
docker compose up -d                                        # postgres:16 en :5432, base iclinic_db
./gradlew bootRun                                           # perfil por defecto: postgres
SPRING_PROFILES_ACTIVE=postgres,dev ./gradlew bootRun       # + datos de prueba
SPRING_PROFILES_ACTIVE=h2 ./gradlew bootRun                 # H2 en memoria, sin Docker
```

Perfiles: el activo se fija en `application.properties` (`spring.profiles.active`,
hoy `postgres`) y la variable de entorno lo sobrescribe sin tocar el fichero.

| Perfil | Base | Esquema | Datos |
|---|---|---|---|
| `postgres` | PostgreSQL 16 | Flyway, `ddl-auto=validate` | ninguno |
| `postgres,dev` | PostgreSQL 16 | Flyway | `db/seed/dev-seed.sql`, idempotente |
| `h2` | H2 en memoria | Hibernate `create-drop` | `import.sql` |
| `test` | H2 | Hibernate `create-drop` | fixtures de cada test |

Migraciones: viven en `src/main/resources/db/migration/` con el patrón
`V<n>__descripcion.sql`, solo SQL de PostgreSQL. Hoy existe `V1__baseline_schema.sql`.
Nunca se edita una migración ya aplicada; se añade una nueva. Los datos de
desarrollo no son una migración: van en `db/seed/dev-seed.sql`, fuera de la
secuencia de Flyway.

Inspección de la base:

```bash
docker exec -it iclinic-postgres psql -U postgres -d iclinic_db
docker exec iclinic-postgres psql -U postgres -d iclinic_db -c "select version, description, success from flyway_schema_history;"
```

## Convenciones de paquetes observadas

Raíz: `com.iclinic.iclinicbackend`.

- `modules/<modulo>/` — organización por funcionalidad, no por capa. Cada módulo
  repite la estructura `controller/`, `service/`, `repository/`, `entity/`,
  `dto/`, `mapper/`, y añade `enums/`, `factory/`, `strategy/`, `adapter/` cuando
  aplica. Módulos actuales: `access`, `admin`, `appointment`, `auth`, `branch`,
  `company`, `crm`, `user`.
- `modules/crm/` anida submódulos con la misma estructura: `channel`, `contact`,
  `conversation`, `message`, `webhook`, más `exception/` propio.
- `config/` — `@Configuration` de la aplicación (seguridad, RestTemplate,
  WebSocket, Firebase, auto-registro de webhook de Telegram).
- `shared/` — `entity/` (`BaseEntity`, `ActivableEntity`), `enums/`, `exception/`,
  `security/`, `utils/`, `validation/`.

Los enums transversales de dominio (`ChannelType`, `ChannelProvider`,
`MessageStatus`, `UserRole`, …) viven en `shared/enums/`, no dentro del módulo.

## Estilo de código

- Java 17, Spring Boot 3.5.13-SNAPSHOT, Gradle. Lombok en todas las clases:
  `@RequiredArgsConstructor` para inyección por constructor (nunca `@Autowired`
  en campos), `@Slf4j` para logs, `@Builder`/`@SuperBuilder` en entidades.
- Entidades JPA con `@Table(name = "...")` explícito y nombres de tabla en
  snake_case. Las relaciones `@ManyToOne` son `FetchType.LAZY`.
- Las variantes por país se modelan con herencia JPA, no con banderas:
  `Company` → `EcuadorianCompany`/`ColombianCompany`; `User` → `EcuadorianUser`,
  `ColombianUser`, `PeruvianUser`, `InternationalUser`. Al persistir un usuario se
  usa siempre una subclase concreta.
- Selección de comportamiento por tipo mediante Strategy + Registry + Factory
  (`CompanyCreationStrategyRegistry`, `CompanyFactory`, `branch/factory/`,
  `MessagingChannelAdapterRegistry`). El registro se autodescubre inyectando
  `List<Interfaz>`: añadir una implementación no requiere tocar el registro.
- Controladores y servicios nunca exponen entidades; convierten con el mapper del
  módulo (`*/mapper/`). Las peticiones se validan con `@Valid` sobre el DTO.
- Errores: se lanzan excepciones de dominio (`CompanyNotFoundException`,
  `ChannelConnectionNotFoundException`, `InvalidChannelConfigurationException`, …)
  y las traduce `shared/exception/GlobalExceptionHandler` al contrato
  `ErrorResponse`. No se construyen `ResponseEntity` de error en línea.
- Baja lógica, no borrado físico: `ActivableEntity.active`, `deletedAt`, endpoints
  `PATCH /{id}/deactivate`.
- Los endpoints se documentan con anotaciones OpenAPI (`@Tag`, `@Operation`,
  `@ApiResponse`) como hace el resto de controladores.
- Idioma: el código, los comentarios, los mensajes de log y los de excepción están
  en español. Se mantiene.

## Reglas innegociables

1. Toda consulta que devuelva datos de una empresa filtra por `company_id`. Un
   identificador que venga del cliente o del payload de un proveedor no se usa
   nunca como prueba de pertenencia a un tenant: se resuelve contra la base.
2. Ningún cambio de estructura de base de datos sin migración Flyway versionada.
   No se reintroduce `ddl-auto=update` ni `create-drop` en `postgres`.
3. Los secretos (tokens de bot, tokens de acceso de Meta, app secrets) se guardan
   cifrados mediante `SecretEncryptionService` y se leen de variables de entorno.
   No se versiona ningún secreto real, ni en `application*.properties`, ni en
   `dev-seed.sql`, ni en tests.
4. Los endpoints de webhook son públicos en `SecurityConfig`; su autenticidad
   depende de la verificación de firma o secreto, no de la red. Quien toque un
   webhook mantiene esa verificación.
5. No se rompe un contrato REST existente sin dejarlo anotado como obsoleto
   durante una fase y documentar el cambio. Los módulos `appointment`, `user`,
   `branch` y `company` no se tocan al trabajar en CRM.
6. No se añaden dependencias nuevas (colas, cachés, brokers, Testcontainers) sin
   aprobación explícita.
7. Tests antes que implementación en cada tarea, y el test debe fallar por la
   razón correcta antes de escribir el código.

## Verificación obligatoria al terminar cualquier tarea

Ninguna tarea está terminada hasta que, en este orden:

1. `./gradlew build` compila y la suite completa pasa. Se reporta el número de
   tests ejecutados y de fallos, con la salida real. Un test que se salta o se
   ignora se declara.
2. Si la tarea toca entidades o esquema: la aplicación arranca con el perfil
   `postgres` sobre una base limpia, Flyway aplica sin errores y `ddl-auto=validate`
   pasa. Eso es lo que prueba que la migración coincide con las entidades.
3. No se rompen endpoints existentes: los que la tarea toca se ejercitan con la
   petición real y se registra la respuesta; los demás siguen expuestos con la
   misma ruta, método y forma de respuesta. Un cambio de contrato deliberado se
   anota en el documento de la fase correspondiente.
4. Se declara explícitamente qué quedó fuera y por qué. Reducir el alcance es una
   decisión del responsable del repositorio, no del agente.
