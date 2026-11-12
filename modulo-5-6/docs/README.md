# Integrante 4 — Evaluación y resultados

Alcance: RF-12, RF-13, RF-14, RF-15, RF-16, RF-21, RF-24, RF-26, RF-27.

## Stack

Las versiones del equipo, en el backend único (ver el [README de la raíz](../../README.md)):

- Java 21 y Spring Boot 4.1.2 (antes Java 17 y Spring Boot 3.4.5)
- MySQL 8 con Flyway: esquema en `modulo-5-6/backend/src/main/resources/db/migration/V40__modulo56_evaluacion.sql`
- Spring Mail (`JavaMailSender`)
- OpenPDF 2.0.3 (MPL-2.0 / LGPL-2.1+) para PDF
- Apache POI 5.4.1 (Apache License 2.0) para Excel

Las pruebas en memoria usan H2 en `MODE=MySQL` (`aplicacion/src/test/resources/application-test.yml`).

## Decisiones funcionales

1. Cada estudiante tiene **un intento**, y un segundo solo si el docente creador del caso autoriza una repetición
   (RF-25, módulo 4): **máximo dos intentos** por autorización. El historial de cada intento es independiente.
2. Una **ausencia** se registra con **nota 0** y permanece **NO_INICIADA**. No es un intento incompleto: la incompleta sale de un intento **EN_CURSO** que se cierra como **INCOMPLETA** con las respuestas que hubiera. La ausencia no reabre ese intento ni impide abrir el segundo.
3. El **docente selecciona** cuál intento será la **nota oficial** (`POST /api/resultados/nota-oficial`).
4. Esa selección **no modifica** `notaFinal` de ningún intento.
5. Si el docente **no ha elegido** nota oficial, **no** se asume la nota mayor ni la más reciente.
6. Notificaciones por correo Spring Boot.
7. `nota = (peso_obtenido / peso_total) * rubrica.escala_maxima` (100 si no hay escala).
8. RN17 / RF-22: el ingreso tardío lo decide **una sola regla**, la del módulo 4 (`PoliticaIngresoTardio`: hasta la
   mitad del tiempo máximo contado desde el inicio de la sesión). Este módulo la consulta por el puerto
   `PoliticaInicioIntento` antes de abrir un intento nuevo (antes tenía su propia regla, `Rn17`, distinta).

## Estado de los requisitos

| RF | Estado | Notas |
|---|---|---|
| RF-12 | Implementado | Inicio y fin; regla de ingreso del módulo 4 (RF-22) e intentos permitidos (RF-25). State: `EstadoDeParticipacion` y estados concretos. |
| RF-13 | Implementado | Nota con rúbrica. Strategy + `FachadaEvaluacion.calcular`, que delega en `CalculoNota` (no reescribe una nota persistida). |
| RF-14 | Implementado | Consulta de resultado cerrado, nota oficial elegida por el docente. State + Facade + Proxy. |
| RF-15 | Implementado | Consulta e historial de participaciones. Facade + `ProxyConsultaParticipacion`. |
| RF-16 | Implementado | PDF OpenPDF 2.0.3 y Excel Apache POI 5.4.1. Facade + `ProxyDescargaReporte`. |
| RF-21 | Implementado | Convocatoria y cambios. Observer (`PublicadorEventos`) + `FachadaNotificaciones`. |
| RF-24 | Implementado | Incompleta ≠ ausencia (nota 0). Strategy de completa, incompleta y ausencia. |
| RF-26 | Implementado | Retroalimentación por intento. Facade + `ProxyRetroalimentacion`. |
| RF-27 | Implementado | Aviso de calificaciones vía `FachadaNotificaciones` (el cálculo sigue en `FachadaEvaluacion`) y aviso de feedback. Observer + Facade. |

Patrones de la tabla 4.1 aplicados en este módulo: State, Strategy, Facade, Proxy y Observer. El API valida un JWT HS256; no emite sesiones ni trae el catálogo académico real. Existe un frontend Angular 21 en `frontend/` todavía **sin versionar** en esta rama; solo RF-15 (consulta de participaciones) llama al API, todavía sin `Authorization`, y el resto son pantallas de estructura.

## Relación tabla 4.1 y clases reales

Nombres comprobados en el código de esta rama. La columna **4.1** es lo exigido; **Adicional** es coordinación que existe en la arquitectura pero no está pedida para ese RF.

| RF | 4.1 | Clases del patrón exigido | Responsabilidad | Adicional (no exigido por 4.1) |
|---|---|---|---|---|
| RF-12 | State | `EstadoDeParticipacion`, `EstadoNoIniciada`, `EstadoEnCurso`, `EstadoFinalizada`, `EstadoIncompleta`, `MaquinaEstadoParticipacion` | Cada estado admite o rechaza iniciar, finalizar o marcar incompleta. | `FachadaParticipacion` orquesta inicio/fin. `Participacion.setEstado` es de paquete: solo la máquina cambia un intento ya creado. `NO_INICIADA` es el valor inicial del campo; `enEstadoInicial` no es una transición. |
| RF-13 | Strategy y Facade | `EstrategiaEvaluacion`, `EstrategiaEvaluacionCompleta`, `CalculoRubrica`, `FachadaEvaluacion`, `CalculoNota` | La fachada delega en `CalculoNota`, que elige la estrategia y persiste la nota; no reescribe un `Resultado` existente. | — |
| RF-14 | State, Facade y Proxy | State: los mismos estados de RF-12 (`consultarCerrado` no entrega un intento `EN_CURSO`). Facade: `FachadaEvaluacion`, `LecturaResultados`, `ServicioNotaOficial`. Proxy: `ConsultaResultado`, `ProxyConsultaResultado` | El Proxy limita visibilidad. La fachada delega la lectura y la nota oficial elegida por el docente. | — |
| RF-15 | Facade y Proxy | `FachadaParticipacion`, `ConsultaParticipacion`, `ProxyConsultaParticipacion` | El Proxy filtra dueño/práctica; el Facade describe e historializa intentos. | — |
| RF-16 | Facade y Proxy | `FachadaReportes`, `DescargaReporte`, `ProxyDescargaReporte` | El Proxy autoriza la descarga; el Facade arma filas y PDF/Excel sin recalcular notas. | Generadores `ReportePdfOpenPdf` y `ReporteExcelPoi` (no son el Proxy). |
| RF-21 | Observer y Facade | Observer: `PublicadorEventos`, `ObservadorAcademico`, `ObservadorCorreo`. Facade: `FachadaNotificaciones` | El Facade publica convocatoria/cambio; el Observer envía correo con idempotencia. | `ServicioCorreoNotificaciones` persiste el intento SMTP; no es un segundo observador. |
| RF-24 | Strategy | `EstrategiaEvaluacion`, `EstrategiaEvaluacionIncompleta`, `EstrategiaEvaluacionAusencia`, `EstrategiaEvaluacionCompleta` | Incompleta usa respuestas registradas; ausencia deja nota 0 en `NO_INICIADA`. | `CierrePendientes` (vía `FachadaEvaluacion.evaluarPendientes`) y el State (`marcarIncompleta`) coordinan el cierre; 4.1 no pide Facade ni State en este RF. |
| RF-26 | Facade y Proxy | `FachadaRetroalimentacion`, `GestionRetroalimentacion`, `ProxyRetroalimentacion` | El Proxy restringe registro y consulta; el Facade guarda el texto por intento. | Al guardar se publica un evento (eso es RF-27, no un patrón extra de RF-26). |
| RF-27 | Observer y Facade | Observer: `PublicadorEventos`, `ObservadorAcademico`, `ObservadorCorreo`. Facade: `FachadaNotificaciones.publicarCalificaciones`; el feedback sale de `FachadaRetroalimentacion.guardar` | Avisa calificaciones ya persistidas y retroalimentación; no altera `notaFinal`. | `FachadaEvaluacion.publicarCalificaciones` solo delega en `FachadaNotificaciones`. |

Los tipos de `contrato` son los puertos del módulo hacia los demás; la aplicación los conecta en `aplicacion/.../integracion`
(detalle en `modulo-5-6/backend/README.md`). En las pruebas del módulo se usan dobles en memoria.

## Actor y visibilidad por rol

El actor sale del token del módulo 1 (`integracion.ActorDesdeSesion`): id de usuario, rol (`ESTUDIANTE`, `DOCENTE` o
`ADMINISTRADOR`) y permisos vigentes en la base. Sin token, o con token inválido o vencido, la ruta responde 401;
sin el permiso del módulo 1 que pide la operación, 403. En la base única `estudianteId` y `docenteId` son el id de usuario.

La visibilidad de consulta (`AccesoEvaluacion.visible` y, en notificaciones, el filtro de `FachadaNotificaciones`) es:

| Recurso | Estudiante | Docente | Restricción cruzada |
|---|---|---|---|
| Participaciones | Solo las suyas (`estudianteId`) | Solo prácticas con su `docenteId` | Otro estudiante: 404. Docente de otra práctica: listado vacío / 404 |
| Resultados y notas oficiales | Solo las suyas | Solo las de sus prácticas | Igual. Un resultado `EN_CURSO` no se consulta aunque sea propio |
| Reportes PDF/Excel | Solo filas propias; `?estudiante=` de otro es 403 | `?practica=` de otra práctica es 403; sin filtro, solo sus prácticas | El reporte no mezcla grupos ajenos |
| Notificaciones | Solo las dirigidas a su `estudianteId` | Solo las de prácticas propias | No ve convocatoria/cambio/calificación de otro |
| Retroalimentaciones | Solo las de sus intentos | Solo las de sus prácticas | GET ajeno: 404. Registrar: solo docente (o admin) de esa práctica |

Mutaciones de intento (responder, finalizar e iniciar): **solo el estudiante dueño**. El docente no responde ni finaliza en nombre del estudiante.

`ADMINISTRADOR` en código ve todos los recursos visibles y puede operar como docente de la práctica; no implica un módulo de administración entregado.

## RF-13 / RF-14 / RF-24

- Fórmula: `(peso_obtenido / peso_total) * rubrica.escala_maxima`. Si no hay rúbrica, escala = 100.
- Un `Resultado` por participación (intento). Un reintento crea otra participación y otro resultado; no se reescribe el anterior.
- Al evaluar pendientes: `EN_CURSO` pasa a `INCOMPLETA` (nota con respuestas registradas); autorización sin inicio queda `NO_INICIADA` con resultado de ausencia. La distinción de negocio está en **Decisiones funcionales**.
- Solo el estudiante dueño responde y finaliza. `calcular` no reescribe una nota ya persistida.
- Consulta: estudiante solo sus resultados; docente solo prácticas con `docenteId` propio.
- La nota oficial y el tope de dos intentos están en **Decisiones funcionales**; no se repetirán aquí.

## Endpoints RF-12 / RF-15 / RF-13 / RF-14 / RF-24

| Método | Ruta | Quién |
|---|---|---|
| POST | `/api/participaciones/iniciar` | Estudiante. Body `{ "autorizacionId": n }` |
| POST | `/api/participaciones/{id}/finalizar` | Estudiante dueño |
| GET | `/api/participaciones/{id}` | Dueño o docente de la práctica |
| PUT | `/api/participaciones/{id}/preguntas/{idPregunta}/respuesta` | Registrar o cambiar la opción (módulo 4, RF-10 y RF-11). Body `{ idRespuesta }` |
| GET | `/api/participaciones` | Listado filtrable (`practica`, `estudiante`, `estado`, `numeroIntento`, `fechaDesde`, `fechaHasta`) |
| GET | `/api/resultados` | Listado de resultados del actor |
| GET | `/api/resultados/{id}` | Detalle consultable si el intento está cerrado |
| POST | `/api/resultados/practica/{practicaId}/evaluar-pendientes` | Docente: incompletas y ausencias |
| POST | `/api/resultados/nota-oficial` | Docente. Body `{ practicaId, estudianteId, participacionId }` |
| GET | `/api/resultados/notas-oficiales` | Consulta de la nota oficial seleccionada |
| GET | `/api/reportes/evaluacion.pdf` | Descarga PDF (`practica`, `estudiante` opcionales) |
| GET | `/api/reportes/evaluacion.xlsx` | Descarga Excel. Una fila por intento |
| POST | `/api/notificaciones/practica/{id}/convocatoria` | Docente: correo de convocatoria (idempotente) |
| POST | `/api/notificaciones/practica/{id}/cambio` | Docente. Body `{ "motivo": "FECHAS" }` |
| GET | `/api/notificaciones` | Estudiante: las suyas. Docente: las de sus prácticas |
| POST | `/api/retroalimentaciones` | Docente. Body `{ participacionId, texto }` |
| GET | `/api/retroalimentaciones` | Consulta filtrable (`practica`, `participacion`) |
| GET | `/api/retroalimentaciones/{id}` | Detalle si el actor puede ver la participación |
| POST | `/api/resultados/practica/{id}/publicar-calificaciones` | Docente: avisa notas por intento, sin duplicar |

## Pruebas

Desde la raíz del repositorio:

```bash
mvn test -Dtest='co.edu.humboldt.plataformacasos.evaluacion.**.*Test'
```

61 pruebas unitarias y de API sobre H2, con dobles en memoria de los puertos (`soporte/DoblesModuloEvaluacion`).
La integración real con los demás módulos está en `FlujoCompletoTest` (raíz `aplicacion/`). Detalle en
`modulo-5-6/backend/README.md`.

## Integración (resuelta)

- Catálogo: `CatalogoAcademicoEnMemoria` pasó a ser un doble de prueba; el catálogo real es
  `integracion.CatalogoAcademicoIntegrado` (sesiones y autorizaciones del módulo 3, estudiantes del módulo 1,
  preguntas, opciones correctas y rúbrica del módulo 2).
- Seguridad: se eliminaron `SecurityConfig`, `JwtConfig` y `JWT_SECRET`; vale el token del módulo 1 (`JWT_SECRETO`).
  CORS lo configura el módulo 1 (`CORS_ORIGENES`).
- Persistencia: Flyway aplica `V40__modulo56_evaluacion.sql`; ya no se aplica a mano.
- Las convocatorias y cambios se envían solos cuando el módulo 3 convoca o modifica una sesión, y la evaluación de
  pendientes (RF-24) se hace sola al finalizar la sesión. Los endpoints manuales siguen disponibles.
- Se quitó `POST /api/participaciones/{id}/respuestas`: duplicaba RF-10 del módulo 4.
- Nombres alineados con el equipo: `*Controller` → `*Controlador`, `*Repository` → `*Repositorio`, `*Facade` →
  `Fachada*`, `*Proxy` → `Proxy*`, `Evaluacion*Strategy` → `EstrategiaEvaluacion*`; paquete
  `com.unihumboldt.nuclear.evaluacion` → `co.edu.humboldt.plataformacasos.evaluacion`.

## Pendiente

- El frontend Angular de este módulo no está en el repositorio.
- SMTP real: en desarrollo los correos llegan a Mailpit (`docker compose up -d`, http://localhost:8025).
- OpenPDF 2.0.3 (MPL-2.0 / LGPL-2.1+): ya estaba en el esqueleto; si el equipo exige licencia Apache exclusiva, cambiar
  a PDFBox (Apache 2.0).
