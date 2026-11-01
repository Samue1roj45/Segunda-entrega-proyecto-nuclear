# Backend — Evaluación y resultados

Módulo Spring Boot del Integrante 4 (RF-12 a RF-27). No incluye autenticación institucional, catálogo académico real ni frontend.

## Requisitos previos

- JDK 17
- Maven 3.9+ en el `PATH`, o el Maven portable del repositorio: `tools/apache-maven-3.9.9` (esa carpeta está en `.gitignore`; si no está, usa Maven instalado)
- Docker Compose, solo si vas a levantar PostgreSQL 16
- Las pruebas **no** necesitan Docker: usan el perfil `test` con H2 en memoria

## Entorno sin secretos

1. En la **raíz del repositorio** copia la plantilla pública:

```powershell
Copy-Item .env.example .env
```

2. Completa `.env` en local. **No subas `.env`** (está en `.gitignore`).
3. No pegues contraseñas, tokens ni secretos reales en este README ni en el chat.

Variables que usa el arranque (nombres; los valores viven en tu `.env`):

- `SERVER_PORT` — por defecto `8080` (`application.yml`)
- `POSTGRES_HOST`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `POSTGRES_PORT` — puerto **JDBC del host**. En `application.yml` el valor por defecto es `5435`
- `POSTGRES_HOST_PORT` — puerto publicado por Compose hacia el contenedor (`5435:5432` si no se cambia)
- `MAIL_*` — SMTP; las pruebas mockean `JavaMailSender` y no envían correo

`JWT_SECRET` y `JWT_EXPIRATION_MS` aparecen en la plantilla y en `application.yml`, pero **JWT no está integrado**: el actor se resuelve con cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id` y `X-Docente-Id`.

Aviso de puertos: `.env.example` trae `POSTGRES_PORT=5432` (puerto interno del contenedor). Compose publica `5435` en el host y el JDBC por defecto es `5435`. Para hablar con Docker desde la máquina, alinea `POSTGRES_PORT` con `POSTGRES_HOST_PORT` (típicamente `5435`).

## PostgreSQL (opcional para desarrollo)

Desde la raíz del repositorio:

```powershell
docker compose up -d db
```

Imagen: `postgres:16-alpine`. Healthcheck interno: `pg_isready`.

Hibernate está en `ddl-auto: none` y **no hay Flyway** en este módulo. El proceso puede arrancar, pero las operaciones JPA fallarán hasta que exista el esquema. Las pruebas no usan esta base.

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

Todas las pruebas del módulo (perfil `test`, H2, sin SMTP real):

```powershell
mvn -f backend/pom.xml test
```

Con el Maven portable:

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml test
```

Desde `backend/`:

```powershell
..\tools\apache-maven-3.9.9\bin\mvn.cmd test
```

## Estado básico del servicio

`GET /api/health` está permitido sin actor (`SecurityConfig`). Ejemplo:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/health -UseBasicParsing
```

Respuesta esperada (cuerpo JSON): `status` = `UP`, `modulo` = `evaluacion-resultados`, y el listado de RF del alcance. Este endpoint no comprueba PostgreSQL, JWT ni el catálogo real.

## Endpoints del módulo (código actual)

No existen las rutas Django `backend/apps/participaciones|resultados|reportes`. El API es Spring Boot (`*Controller` en `com.unihumboldt.nuclear.evaluacion`). Spring Security exige usuario autenticado en todas las rutas de abajo (`/api/health` es la única pública). El rol de negocio no va en JWT: se lee de `X-Rol` (`ESTUDIANTE`, `DOCENTE`, `ADMIN`) junto con `X-Usuario-Id`, `X-Estudiante-Id` y `X-Docente-Id`.

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
