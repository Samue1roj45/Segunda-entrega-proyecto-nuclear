# Backend — Evaluación y resultados

Módulo Spring Boot del Integrante 4 (RF-12 a RF-27). No incluye autenticación institucional, catálogo académico real ni frontend versionado.

## Pila verificada

- Java 17 (`java.version` en `pom.xml`)
- Spring Boot 3.4.5 (Web, Validation, Data JPA, Security, Mail)
- MySQL 8 (`mysql-connector-j`; Docker Compose con `mysql:8.0`)
- OpenPDF 2.0.3 y Apache POI 5.4.1 para reportes
- Pruebas: JUnit 5, Spring Security Test, H2 (perfil `test`), Testcontainers MySQL (`*IT`)

No se usa Python, Django ni PostgreSQL. Las pruebas en memoria usan H2 en `MODE=MySQL` (`src/test/resources/application-test.yml`).

## Requisitos previos

- JDK 17
- Maven 3.9+ en el `PATH`, o el Maven portable del repositorio: `tools/apache-maven-3.9.9` (esa carpeta está en `.gitignore`; si no está, usa Maven instalado)
- Docker Desktop, solo para: (a) levantar MySQL con Compose, (b) ejecutar las pruebas de integración MySQL (`mvn verify`)
- `mvn test` **no** necesita Docker: usa el perfil `test` con H2 en memoria

## Entorno sin secretos

1. En la **raíz del repositorio** copia la plantilla pública:

```powershell
Copy-Item .env.example .env
```

2. Completa `.env` en local. **No subas `.env`** (está en `.gitignore`).
3. No pegues contraseñas, tokens ni secretos reales en este README ni en el chat.

Variables que usa el arranque (nombres; los valores viven en tu `.env`):

| Variable | Uso | Valor por defecto si falta |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP del backend | `8080` |
| `MYSQL_HOST` | Host JDBC | `localhost` |
| `MYSQL_PORT` | Puerto JDBC **del host** | `5435` |
| `MYSQL_DB` | Base de datos | `evaluacion_db` |
| `MYSQL_USER`, `MYSQL_PASSWORD` | Credenciales de la aplicación | existen valores por defecto en `application.yml`; defínelos siempre en `.env` |
| `MYSQL_ROOT_PASSWORD` | Solo lo usa Compose para inicializar el contenedor | — |
| `MYSQL_HOST_PORT` | Puerto que Compose publica en el host (`MYSQL_HOST_PORT:3306`) | `5435` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP; las pruebas mockean `JavaMailSender` y no envían correo | `localhost`, `1025`, vacío, vacío, `no-reply@simulador.local` |
| `JWT_SECRET` | Clave HS256 con la que `JwtConfig` valida el Bearer. La comparte el módulo de usuarios, que es quien emite el token. Mínimo 32 bytes | placeholder de desarrollo en `application.yml`; en despliegue debe definirse en el entorno |
| `JWT_EXPIRATION_MS` | La usa el emisor al firmar. Este módulo no abre sesión: rechaza un token vencido por su claim `exp` | `3600000` |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring. El perfil `test` solo existe en el classpath de pruebas | — |

`MYSQL_PORT` (JDBC) y `MYSQL_HOST_PORT` (Compose) deben coincidir si el backend corre fuera de Docker y la base dentro; por defecto ambos son `5435`.

## MySQL 8 con Docker Compose

Desde la raíz del repositorio:

```powershell
docker compose up -d db
```

Servicio `db`: imagen `mysql:8.0`, contenedor `evaluacion_db`, puerto `${MYSQL_HOST_PORT:-5435}:3306`, healthcheck `mysqladmin ping`, volumen `mysql_data`.

### Aplicar el esquema `V1__crear_tablas_evaluacion.sql`

Hibernate está en `ddl-auto: none` y el módulo **no incluye Flyway ni otro mecanismo de migración**. Compose **no aplica el SQL automáticamente**: hay que ejecutarlo una vez sobre una base nueva. El backend arranca sin el esquema, pero toda operación JPA fallará hasta aplicarlo.

Desde la raíz del repositorio, en Windows PowerShell 5.1. No hace falta un cliente `mysql` en el host. `Get-Content -Raw` envía el SQL por stdin y `docker exec -i` lo entrega al cliente dentro de `evaluacion_db`. La contraseña no se lee por ese stdin: el `sh` del contenedor expande `MYSQL_USER`, `MYSQL_PASSWORD` y `MYSQL_DATABASE`, variables que Compose ya inyectó. El token `--%` es necesario en Windows PowerShell 5.1: sin él, PowerShell elimina las comillas internas al invocar a Docker y la contraseña quedaría sin comillar. No imprime el valor de ninguna variable. MySQL puede avisar que la contraseña va en su línea de comandos; el aviso no incluye el valor.

```powershell
Get-Content -Path backend\src\main\resources\db\mysql\V1__crear_tablas_evaluacion.sql -Raw | docker exec -i evaluacion_db --% sh -c "mysql --user=\"$MYSQL_USER\" --password=\"$MYSQL_PASSWORD\" --database=\"$MYSQL_DATABASE\""
```

En PowerShell, `<` no redirige un archivo. `mysql -p` sin valor pediría la contraseña por el mismo stdin que el SQL, así que esa variante no sirve.

### Verificar que el esquema quedó creado

```powershell
docker exec -it evaluacion_db mysql -u evaluacion_user -p evaluacion_db -e "SHOW TABLES;"
docker exec -it evaluacion_db mysql -u evaluacion_user -p evaluacion_db -e "SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME, CONSTRAINT_TYPE;"
```

Resultado esperado: 6 tablas (`participacion`, `respuesta_seleccionada`, `resultado`, `nota_oficial`, `notificacion`, `retroalimentacion`) y las restricciones `uk_participacion_autorizacion_intento`, `uk_respuesta_participacion_pregunta`, `uk_resultado_participacion`, `uk_nota_oficial_practica_estudiante`, `uk_notificacion_clave`, `uk_retroalimentacion_participacion`, `fk_respuesta_seleccionada_participacion`, `fk_resultado_participacion`, `fk_nota_oficial_participacion`, `fk_retroalimentacion_participacion`.

### Advertencias sobre el volumen `mysql_data`

- La imagen oficial de MySQL solo ejecuta scripts de `docker-entrypoint-initdb.d` **al crear un volumen vacío**. Montar el SQL ahí no afectaría a una base ya inicializada; por eso hoy la aplicación del esquema es manual.
- **No elimines ni recrees `mysql_data`** (`docker compose down -v`, `docker volume rm`) para "aplicar cambios": destruye los datos locales. Aplica los cambios de esquema con sentencias SQL sobre la base existente.
- Si el equipo decide automatizar la inicialización (montaje en `initdb.d` o migraciones), debe acordarse por separado.

## Arranque del backend

Desde la raíz:

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml spring-boot:run
```

O, con Maven en el `PATH`:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

Desde `backend/`:

```powershell
mvn spring-boot:run
```

Puerto: `http://localhost:8080` salvo que cambies `SERVER_PORT`.

## Pruebas

### Unitarias y de API (H2, sin Docker)

```powershell
mvn -f backend/pom.xml test
```

Con el Maven portable: `.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml test`. Desde `backend/`: `..\tools\apache-maven-3.9.9\bin\mvn.cmd test`.

Usan `src/test/resources/application-test.yml` (perfil `test`, H2 en memoria, `ddl-auto: create-drop`, SMTP mockeado). Ese archivo está en el classpath de pruebas y **no** se empaqueta en el jar.

### Integración con MySQL real (Testcontainers, requiere Docker)

```powershell
mvn -f backend/pom.xml verify
```

`maven-failsafe-plugin` ejecuta las clases `*IT` en la fase `integration-test`. `ParticipacionMysqlSchemaIT` levanta un contenedor efímero `mysql:8.0`, aplica `V1__crear_tablas_evaluacion.sql` tal cual y comprueba persistencia, claves únicas y foráneas de las seis tablas. No usa la base de Compose ni el volumen `mysql_data`. Para lanzar solo el IT: `mvn -f backend/pom.xml verify -Dtest=NINGUNO -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=ParticipacionMysqlSchemaIT`.

`pom.xml` fija `testcontainers.version` en 1.21.4 (el BOM de Boot 3.4.5 trae 1.20.6) para que Testcontainers hable con Docker Engine API ≥ 1.44 (Docker Desktop reciente).

### Qué cubre cada grupo

- Unitarias: State (`MaquinaEstadoParticipacionTest`), fórmula de rúbrica (`CalculoRubricaTest`), Observer (`PublicadorEventosTest`), generadores PDF/XLSX, los cuatro Proxies, claims del actor (`ActorJwtResolverTest`).
- API (`@SpringBootTest` + MockMvc): RF-12 a RF-27 con permisos por rol, idempotencia de notificaciones, error SMTP sin pérdida de datos. `JwtAutenticacionTest` cubre token firmado (200), ausente, inválido o vencido (401), y cabeceras `X-*` que no conceden rol (401 o 403).
- Integración MySQL: esquema V1 real.

Las pruebas de API arman el actor con `ActorTokens` (claims `rol`, `usuarioId`, `estudianteId` o `docenteId`). `JwtAutenticacionTest` además firma un Bearer real con `JwtEncoder` y la clave de `application-test.yml`.

## Estado de la seguridad y dependencias de integración

### Autenticación: JWT HS256

`SecurityConfig` deja `GET /api/health` público y exige un Bearer JWT en el resto. `JwtConfig` valida la firma HS256 con `app.jwt.secret` (`JWT_SECRET`). Un token ausente, mal formado o vencido responde `401`. Este módulo no inicia sesión ni crea usuarios: el módulo de usuarios emite el token con la misma clave.

`ActorJwtResolver` lee solo el principal `Jwt` ya validado. Claims: `rol` (`ESTUDIANTE`, `DOCENTE` o `ADMIN`), `usuarioId` (número o texto numérico; si falta, `sub` numérico), y de forma opcional `estudianteId` y `docenteId`. Si el token es válido pero el rol o el usuario no se pueden leer, no hay actor y la operación responde `403`. Las cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id` y `X-Docente-Id` no se leen.

El frontend de esta rama todavía no envía `Authorization`, así que sus llamadas a rutas protegidas reciben `401` hasta que lo haga.

### CORS: no configurado (coordinación con seguridad)

El backend no define CORS y el frontend Angular (`http://localhost:4200`) no usa proxy de desarrollo, así que el navegador bloquea las llamadas a `http://localhost:8080` aunque exista autenticación. La configuración futura debe acordarse con el responsable de seguridad; lo que este módulo necesita:

- Orígenes: `http://localhost:4200` en desarrollo (configurable por variable para otros entornos).
- Métodos: `GET`, `POST`, `OPTIONS`.
- Cabeceras permitidas: `Content-Type`, `Authorization`.
- Cabeceras expuestas: `Content-Disposition` (nombre de archivo de los reportes PDF/XLSX).

### Catálogo académico

`CatalogoAcademico` es un puerto hacia prácticas, autorizaciones, estudiantes, preguntas, respuestas y rúbricas. La única implementación es `CatalogoAcademicoEnMemoria`, que arranca vacía y solo la pueblan las pruebas. Sin el adaptador al módulo académico no es posible iniciar participaciones ni calcular notas en un despliegue real.

### Frontend

Existe una aplicación Angular 21 en `frontend/` que **todavía no está versionada** en esta rama. Solo la consulta de participaciones (RF-15) llama al API; el resto de pantallas son estructura visual. Su contrato (`ParticipacionRespuesta` en camelCase y los filtros de `GET /api/participaciones`) coincide con los records de este backend.

## Estado básico del servicio

`GET /api/health` está permitido sin actor (`SecurityConfig`). Ejemplo:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/health -UseBasicParsing
```

Respuesta esperada (cuerpo JSON): `status` = `UP`, `modulo` = `evaluacion-resultados`, y el listado de RF del alcance. Este endpoint no comprueba MySQL, autenticación ni el catálogo real.

## Endpoints del módulo (código actual)

El API es Spring Boot (`*Controller` en `com.unihumboldt.nuclear.evaluacion`). Spring Security exige un JWT Bearer en todas las rutas de abajo (`/api/health` es la única pública). El rol sale de los claims `rol`, `usuarioId`, `estudianteId` y `docenteId`.

La tabla de `docs/10-integrante-4-evaluacion-resultados.md` coincide en método y ruta; aquí se añaden query, body y campos JSON tomados de los records.

Estados de participación en query y respuesta: `NO_INICIADA`, `EN_CURSO`, `FINALIZADA`, `INCOMPLETA`.

JSON de participación (`ParticipacionRespuesta`): `id`, `practicaId`, `practicaNombre`, `estudianteId`, `estudianteCorreo`, `estudianteNombre`, `autorizacionId`, `numeroIntento`, `inicio`, `fin`, `tiempoUsadoSeg`, `estado`.

JSON de resultado (`ResultadoRespuesta`): `id`, `participacionId`, `numeroIntento`, `estudianteId`, `estudianteCorreo`, `estudianteNombre`, `practicaId`, `practicaNombre`, `estadoParticipacion`, `correctas`, `incorrectas`, `noRespondidas`, `pesoObtenido`, `pesoTotal`, `escalaMaxima`, `notaFinal`, `oficial`, `notaOficial`, `fechaCalculo`, `detallePreguntas` (`preguntaId`, `enunciado`, `peso`, `respondida`, `respuestaElegidaId`, `correcta`).

JSON de nota oficial (`NotaOficialRespuesta`): `id`, `practicaId`, `estudianteId`, `participacionId`, `numeroIntento`, `notaOficial`, `docenteId`, `fechaSeleccion`.

JSON de envío (`ResumenEnvio`): `enviadas`, `omitidasDuplicado`, `errores`.

JSON de notificación (`NotificacionRespuesta`): `id`, `tipo` (`CONVOCATORIA`, `CAMBIO_PRACTICA`, `CALIFICACION`, `RETROALIMENTACION`), `practicaId`, `estudianteId`, `participacionId`, `destinatarioCorreo`, `asunto`, `cuerpo`, `estado` (`PENDIENTE`, `ENVIADA`, `ERROR`), `errorMensaje`, `fechaCreacion`, `fechaEnvio`.

JSON de retroalimentación (`RetroalimentacionRespuesta`): `id`, `participacionId`, `numeroIntento`, `estudianteId`, `practicaId`, `docenteId`, `texto`, `fechaCreacion`, `fechaActualizacion`.

### Participaciones — `ParticipacionController`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/participaciones` | RF-15 | Actor resuelto; listado filtrado por visibilidad | Query opcional: `practica`, `estudiante`, `estado`, `numeroIntento`, `fechaDesde`, `fechaHasta` (ISO date) | — | Listar intentos visibles | Array de `ParticipacionRespuesta` |
| POST | `/api/participaciones/iniciar` | RF-12 | Solo `ESTUDIANTE`; la autorización debe ser suya | — | `{ "autorizacionId": number }` (`@NotNull`) | Abrir o reutilizar el intento en curso | `ParticipacionRespuesta` |
| GET | `/api/participaciones/{id}` | RF-15 | Dueño o docente de esa práctica (ajeno: 404) | Path: `id` | — | Consultar un intento | `ParticipacionRespuesta` |
| POST | `/api/participaciones/{id}/respuestas` | RF-13 (registrar opción) | Solo el estudiante dueño | Path: `id` | `{ "preguntaId": number, "respuestaId": number }` | Guardar la opción de una pregunta | Cuerpo vacío (`void`) |
| POST | `/api/participaciones/{id}/finalizar` | RF-12 | Solo el estudiante dueño | Path: `id` | — | Cerrar el intento y calcular nota si no existía | `ParticipacionRespuesta` |

### Resultados — `ResultadoController`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/resultados` | RF-14 | Visibilidad estudiante/docente | Query opcional: `practica`, `estudiante` | — | Listar resultados visibles | Array de `ResultadoRespuesta` |
| GET | `/api/resultados/notas-oficiales` | RF-14 | Igual | Query opcional: `practica`, `estudiante` | — | Listar la nota oficial ya elegida | Array de `NotaOficialRespuesta` |
| POST | `/api/resultados/nota-oficial` | RF-14 | Solo `DOCENTE` o `ADMIN`; el docente debe ser de la práctica | — | `{ "practicaId", "estudianteId", "participacionId" }` (`@NotNull`) | Elegir el intento oficial; no cambia `notaFinal` de cada intento | `NotaOficialRespuesta` |
| GET | `/api/resultados/{id}` | RF-14 | Visible para el actor; si el intento está `EN_CURSO`, regla de negocio | Path: `id` | — | Detalle de un resultado cerrado | `ResultadoRespuesta` |
| POST | `/api/resultados/practica/{practicaId}/evaluar-pendientes` | RF-24 | Docente (o admin) de esa práctica | Path: `practicaId` | — | Cerrar incompletas y registrar ausencias con nota 0 | `{ "incompletas", "ausencias", "omitidas" }` |
| POST | `/api/resultados/practica/{practicaId}/publicar-calificaciones` | RF-27 | Docente (o admin) de esa práctica | Path: `practicaId` | — | Avisar notas ya persistidas (idempotente por intento) | `ResumenEnvio` |

### Reportes — `ReporteController`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/reportes/evaluacion.pdf` | RF-16 | Estudiante: solo propio (`?estudiante=` ajeno → 403). Docente: `?practica=` de otra práctica → 403 | Query opcional: `practica`, `estudiante` | — | Descargar PDF | `200` binario `application/pdf`, archivo `reporte-evaluacion.pdf` |
| GET | `/api/reportes/evaluacion.xlsx` | RF-16 | Igual | Query opcional: `practica`, `estudiante` | — | Descargar Excel (una fila por intento) | `200` binario `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, archivo `reporte-evaluacion.xlsx` |

### Notificaciones — `NotificacionController`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/notificaciones` | RF-21, RF-27 | Estudiante: las suyas. Docente: las de sus prácticas | Query opcional: `practica` | — | Consultar avisos persistidos | Array de `NotificacionRespuesta` |
| POST | `/api/notificaciones/practica/{practicaId}/convocatoria` | RF-21 | Docente (o admin) de esa práctica | Path: `practicaId` | — | Correo de convocatoria (idempotente) | `ResumenEnvio` |
| POST | `/api/notificaciones/practica/{practicaId}/cambio` | RF-21 | Igual | Path: `practicaId` | `{ "motivo": string }` (`@NotBlank`) | Correo de cambio de práctica | `ResumenEnvio` |

### Retroalimentación — `RetroalimentacionController`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| POST | `/api/retroalimentaciones` | RF-26 | Solo `DOCENTE` o `ADMIN` de la práctica del intento | — | `{ "participacionId": number, "texto": string }` (`@NotNull` / `@NotBlank`) | Registrar o actualizar el texto del intento | `RetroalimentacionRespuesta` |
| GET | `/api/retroalimentaciones` | RF-26 | Visibilidad sobre la participación | Query opcional: `practica`, `participacion` | — | Listar comentarios visibles | Array de `RetroalimentacionRespuesta` |
| GET | `/api/retroalimentaciones/{id}` | RF-26 | Igual (ajeno: 404) | Path: `id` | — | Consultar un comentario | `RetroalimentacionRespuesta` |
