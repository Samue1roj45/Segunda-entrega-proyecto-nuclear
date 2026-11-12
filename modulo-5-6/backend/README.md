# Backend — Evaluación y resultados (módulos 5 y 6)

Código del Integrante 4 (RF-12 a RF-16, RF-21, RF-24, RF-26, RF-27) dentro del **backend único** del proyecto
(ver el [README de la raíz](../../README.md)): Java 21, Spring Boot 4.1.2, Spring Data JPA, Flyway, MySQL 8,
Spring Mail, OpenPDF 2.0.3 y Apache POI 5.4.1.

## Ejecutar y probar

Desde la raíz del repositorio:

```bash
docker compose up -d                                   # MySQL 8 y Mailpit (correos en http://localhost:8025)
mvn spring-boot:run -Dspring-boot.run.profiles=pruebas # backend con datos ficticios
mvn test -Dtest='co.edu.humboldt.plataformacasos.evaluacion.**.*Test'   # pruebas de este módulo (H2)
```

El esquema es la migración Flyway `src/main/resources/db/migration/V40__modulo56_evaluacion.sql`; se aplica sola al
arrancar (ya no hay que ejecutar el script a mano). Las variables (`DB_URL`, `JWT_SECRETO`, `MAIL_*`...) están en
`.env.example` de la raíz.

## Pruebas del módulo

Las pruebas prueban el módulo aislado: `soporte/DoblesModuloEvaluacion` reemplaza los puertos de `contrato` por
dobles en memoria (`CatalogoAcademicoEnMemoria`, `ActorDeClaimsJwt` y `PoliticaInicioEnMemoria`) y las de API
arman el actor con `ActorTokens` (claims `rol`, `usuarioId`, `estudianteId` o `docenteId`). La integración real con
los demás módulos se prueba en `aplicacion/src/test/.../FlujoCompletoTest` (también sobre MySQL con
`FlujoCompletoMysqlTest`, que valida las entidades contra el esquema).

- Unitarias: State (`MaquinaEstadoParticipacionTest`), fórmula de rúbrica (`CalculoRubricaTest`), Observer
  (`PublicadorEventosTest`), generadores PDF/XLSX y los cuatro Proxies.
- API (`@SpringBootTest` + MockMvc): RF-12 a RF-27 con permisos por rol, idempotencia de notificaciones y error SMTP
  sin pérdida de datos.

## Integración con los demás módulos

| Puerto (`contrato`) | Lo implementa | Qué entrega |
|---|---|---|
| `ActorActualResolver` | `integracion.ActorDesdeSesion` (módulo 1) | Usuario del token con su rol y permisos vigentes |
| `CatalogoAcademico` | `integracion.CatalogoAcademicoIntegrado` | Sesiones y autorizaciones (módulo 3), estudiantes (módulo 1), preguntas, opciones correctas y rúbrica (módulo 2) |
| `PoliticaInicioIntento` | `integracion.PoliticaInicioDesdeSesiones` | Regla de ingreso tardío del módulo 4 (RF-22) |

Este módulo ofrece al módulo 4 el puerto `participacion.RegistroParticipaciones` (intentos, respuestas y cierre por
tiempo, RF-23) y atiende los eventos del módulo 3 a través de `integracion.ObservadorSesiones`: al finalizar una
sesión evalúa los pendientes (RF-24) y al convocar o cambiar una sesión envía el correo (RF-20, RF-21). La
convocatoria lleva el código de acceso solo en el correo; la notificación guardada no lo incluye.

## Endpoints del módulo (código actual)

Controladores `*Controlador` en `co.edu.humboldt.plataformacasos.evaluacion`. Todas las rutas exigen el token del
módulo 1 (`Authorization: Bearer <token>`); el actor (rol, id y permisos) es el usuario de ese token. Errores con el
formato único `{fecha, estado, error, mensaje, ruta}`.

Estados de participación en query y respuesta: `NO_INICIADA`, `EN_CURSO`, `FINALIZADA`, `INCOMPLETA`.

JSON de participación (`ParticipacionRespuesta`): `id`, `practicaId`, `practicaNombre`, `estudianteId`, `estudianteCorreo`, `estudianteNombre`, `autorizacionId`, `numeroIntento`, `inicio`, `fin`, `tiempoUsadoSeg`, `estado`.

JSON de resultado (`ResultadoRespuesta`): `id`, `participacionId`, `numeroIntento`, `estudianteId`, `estudianteCorreo`, `estudianteNombre`, `practicaId`, `practicaNombre`, `estadoParticipacion`, `correctas`, `incorrectas`, `noRespondidas`, `pesoObtenido`, `pesoTotal`, `escalaMaxima`, `notaFinal`, `oficial`, `notaOficial`, `fechaCalculo`, `detallePreguntas` (`preguntaId`, `enunciado`, `peso`, `respondida`, `respuestaElegidaId`, `correcta`).

JSON de nota oficial (`NotaOficialRespuesta`): `id`, `practicaId`, `estudianteId`, `participacionId`, `numeroIntento`, `notaOficial`, `docenteId`, `fechaSeleccion`.

JSON de envío (`ResumenEnvio`): `enviadas`, `omitidasDuplicado`, `errores`.

JSON de notificación (`NotificacionRespuesta`): `id`, `tipo` (`CONVOCATORIA`, `CAMBIO_PRACTICA`, `CALIFICACION`, `RETROALIMENTACION`), `practicaId`, `estudianteId`, `participacionId`, `destinatarioCorreo`, `asunto`, `cuerpo`, `estado` (`PENDIENTE`, `ENVIADA`, `ERROR`), `errorMensaje`, `fechaCreacion`, `fechaEnvio`.

JSON de retroalimentación (`RetroalimentacionRespuesta`): `id`, `participacionId`, `numeroIntento`, `estudianteId`, `practicaId`, `docenteId`, `texto`, `fechaCreacion`, `fechaActualizacion`.

### Participaciones — `ParticipacionControlador`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/participaciones` | RF-15 | Actor resuelto; listado filtrado por visibilidad | Query opcional: `practica`, `estudiante`, `estado`, `numeroIntento`, `fechaDesde`, `fechaHasta` (ISO date) | — | Listar intentos visibles | Array de `ParticipacionRespuesta` |
| POST | `/api/participaciones/iniciar` | RF-12 | Solo `ESTUDIANTE` (`PARTICIPAR_EN_CASOS`); la autorización debe ser suya y estar activa | — | `{ "autorizacionId": number }` (`@NotNull`) | Abrir o retomar el intento en curso. Un intento nuevo exige la regla de ingreso del módulo 4 (RF-22) y que queden intentos (RF-25) | `ParticipacionRespuesta` |
| GET | `/api/participaciones/{id}` | RF-15 | Dueño o docente de esa práctica (ajeno: 404) | Path: `id` | — | Consultar un intento | `ParticipacionRespuesta` |
| POST | `/api/participaciones/{id}/finalizar` | RF-12 | Solo el estudiante dueño | Path: `id` | — | Cerrar el intento y calcular nota si no existía | `ParticipacionRespuesta` |

### Resultados — `ResultadoControlador`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/resultados` | RF-14 | Visibilidad estudiante/docente | Query opcional: `practica`, `estudiante` | — | Listar resultados visibles | Array de `ResultadoRespuesta` |
| GET | `/api/resultados/notas-oficiales` | RF-14 | Igual | Query opcional: `practica`, `estudiante` | — | Listar la nota oficial ya elegida | Array de `NotaOficialRespuesta` |
| POST | `/api/resultados/nota-oficial` | RF-14 | Solo `DOCENTE` o `ADMINISTRADOR`; el docente debe ser de la práctica | — | `{ "practicaId", "estudianteId", "participacionId" }` (`@NotNull`) | Elegir el intento oficial; no cambia `notaFinal` de cada intento | `NotaOficialRespuesta` |
| GET | `/api/resultados/{id}` | RF-14 | Visible para el actor; si el intento está `EN_CURSO`, regla de negocio | Path: `id` | — | Detalle de un resultado cerrado | `ResultadoRespuesta` |
| POST | `/api/resultados/practica/{practicaId}/evaluar-pendientes` | RF-24 | Docente (o administrador) de esa práctica | Path: `practicaId` | — | Cerrar incompletas y registrar ausencias con nota 0 | `{ "incompletas", "ausencias", "omitidas" }` |
| POST | `/api/resultados/practica/{practicaId}/publicar-calificaciones` | RF-27 | Docente (o administrador) de esa práctica | Path: `practicaId` | — | Avisar notas ya persistidas (idempotente por intento) | `ResumenEnvio` |

### Reportes — `ReporteControlador`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/reportes/evaluacion.pdf` | RF-16 | Estudiante: solo propio (`?estudiante=` ajeno → 403). Docente: `?practica=` de otra práctica → 403 | Query opcional: `practica`, `estudiante` | — | Descargar PDF | `200` binario `application/pdf`, archivo `reporte-evaluacion.pdf` |
| GET | `/api/reportes/evaluacion.xlsx` | RF-16 | Igual | Query opcional: `practica`, `estudiante` | — | Descargar Excel (una fila por intento) | `200` binario `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, archivo `reporte-evaluacion.xlsx` |

### Notificaciones — `NotificacionControlador`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| GET | `/api/notificaciones` | RF-21, RF-27 | Estudiante: las suyas. Docente: las de sus prácticas | Query opcional: `practica` | — | Consultar avisos persistidos | Array de `NotificacionRespuesta` |
| POST | `/api/notificaciones/practica/{practicaId}/convocatoria` | RF-21 | Docente (o administrador) de esa práctica | Path: `practicaId` | — | Correo de convocatoria (idempotente) | `ResumenEnvio` |
| POST | `/api/notificaciones/practica/{practicaId}/cambio` | RF-21 | Igual | Path: `practicaId` | `{ "motivo": string }` (`@NotBlank`) | Correo de cambio de práctica | `ResumenEnvio` |

### Retroalimentación — `RetroalimentacionControlador`

| Método | Ruta | RF | Quién (código de negocio) | Parámetros | Body | Propósito | Respuesta |
|---|---|---|---|---|---|---|---|
| POST | `/api/retroalimentaciones` | RF-26 | Solo `DOCENTE` o `ADMINISTRADOR` de la práctica del intento | — | `{ "participacionId": number, "texto": string }` (`@NotNull` / `@NotBlank`) | Registrar o actualizar el texto del intento | `RetroalimentacionRespuesta` |
| GET | `/api/retroalimentaciones` | RF-26 | Visibilidad sobre la participación | Query opcional: `practica`, `participacion` | — | Listar comentarios visibles | Array de `RetroalimentacionRespuesta` |
| GET | `/api/retroalimentaciones/{id}` | RF-26 | Igual (ajeno: 404) | Path: `id` | — | Consultar un comentario | `RetroalimentacionRespuesta` |

Las respuestas del intento se registran desde el módulo 4: `PUT /api/participaciones/{id}/preguntas/{idPregunta}/respuesta`
(RF-10, RF-11), que usa el puerto `RegistroParticipaciones` de este módulo.

Permisos del módulo 1 que exige cada operación del docente: `CONSULTAR_PARTICIPACIONES` (participaciones,
resultados, nota oficial, evaluar pendientes y publicar calificaciones), `DESCARGAR_REPORTES`,
`REGISTRAR_RETROALIMENTACION` y `PROGRAMAR_SESIONES` (convocatoria y cambio manuales). El estudiante consulta lo suyo
con `CONSULTAR_RESULTADOS_PROPIOS`. El administrador conserva la visibilidad total.
