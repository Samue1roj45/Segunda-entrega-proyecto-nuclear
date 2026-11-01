# Integrante 4 — Evaluación y resultados

Alcance: RF-12, RF-13, RF-14, RF-15, RF-16, RF-21, RF-24, RF-26, RF-27.

## Stack de esta rama

- Java 17
- Spring Boot 3.4.5
- PostgreSQL 16 (Docker Compose)
- Spring Mail (`JavaMailSender`)
- OpenPDF 2.0.3 (MPL-2.0 / LGPL-2.1+) para PDF
- Apache POI 5.4.1 (Apache License 2.0) para Excel

No se usa Python ni Django.

## Decisiones funcionales

1. Cada estudiante tiene **máximo dos intentos** por autorización. El historial de cada intento es independiente.
2. Una **ausencia** se registra con **nota 0** y permanece **NO_INICIADA**. No es un intento incompleto: la incompleta sale de un intento **EN_CURSO** que se cierra como **INCOMPLETA** con las respuestas que hubiera. La ausencia no reabre ese intento ni impide abrir el segundo.
3. El **docente selecciona** cuál intento será la **nota oficial** (`POST /api/resultados/nota-oficial`).
4. Esa selección **no modifica** `notaFinal` de ningún intento.
5. Si el docente **no ha elegido** nota oficial, **no** se asume la nota mayor ni la más reciente.
6. Notificaciones por correo Spring Boot.
7. `nota = (peso_obtenido / peso_total) * rubrica.escala_maxima` (100 si no hay escala).
8. RN17: no iniciar después de la mitad del intervalo de la práctica.

## Estado de los requisitos

| RF | Estado | Notas |
|---|---|---|
| RF-12 | Implementado | Inicio, fin, RN17, dos intentos independientes. State: `EstadoDeParticipacion` y estados concretos. |
| RF-13 | Implementado | Nota con rúbrica. Strategy + `EvaluacionFacade.calcular` (no reescribe una nota persistida). |
| RF-14 | Implementado | Consulta de resultado cerrado, nota oficial elegida por el docente. State + Facade + Proxy. |
| RF-15 | Implementado | Consulta e historial de participaciones. Facade + `ParticipacionConsultaProxy`. |
| RF-16 | Implementado | PDF OpenPDF 2.0.3 y Excel Apache POI 5.4.1. Facade + `ReporteDescargaProxy`. |
| RF-21 | Implementado | Convocatoria y cambios. Observer (`PublicadorEventos`) + `NotificacionFacade`. |
| RF-24 | Implementado | Incompleta ≠ ausencia (nota 0). Strategy de completa, incompleta y ausencia. |
| RF-26 | Implementado | Retroalimentación por intento. Facade + `RetroalimentacionProxy`. |
| RF-27 | Implementado | Aviso de calificaciones vía `NotificacionFacade` (el cálculo sigue en `EvaluacionFacade`) y aviso de feedback. Observer + Facade. |

Patrones de la tabla 4.1 aplicados en este módulo: State, Strategy, Facade, Proxy y Observer. No están integrados JWT, el catálogo académico real ni un frontend en este repositorio.

## Relación tabla 4.1 y clases reales

Nombres comprobados en el código de esta rama. La columna **4.1** es lo exigido; **Adicional** es coordinación que existe en la arquitectura pero no está pedida para ese RF.

| RF | 4.1 | Clases del patrón exigido | Responsabilidad | Adicional (no exigido por 4.1) |
|---|---|---|---|---|
| RF-12 | State | `EstadoDeParticipacion`, `EstadoNoIniciada`, `EstadoEnCurso`, `EstadoFinalizada`, `EstadoIncompleta`, `MaquinaEstadoParticipacion` | Cada estado admite o rechaza iniciar, finalizar o marcar incompleta. | `ParticipacionFacade` orquesta inicio/fin; no sustituye al State. |
| RF-13 | Strategy y Facade | `EstrategiaEvaluacion`, `EvaluacionCompletaStrategy`, `CalculoRubrica`, `EvaluacionFacade` | El Facade elige estrategia y persiste la nota; no reescribe un `Resultado` existente. | — |
| RF-14 | State, Facade y Proxy | State: los mismos estados de RF-12 (`consultarCerrado` no entrega un intento `EN_CURSO`). Facade: `EvaluacionFacade`. Proxy: `ConsultaResultado`, `ResultadoConsultaProxy` | El Proxy limita visibilidad; el Facade arma el resultado y la nota oficial elegida por el docente. | — |
| RF-15 | Facade y Proxy | `ParticipacionFacade`, `ConsultaParticipacion`, `ParticipacionConsultaProxy` | El Proxy filtra dueño/práctica; el Facade describe e historializa intentos. | — |
| RF-16 | Facade y Proxy | `ReporteFacade`, `DescargaReporte`, `ReporteDescargaProxy` | El Proxy autoriza la descarga; el Facade arma filas y PDF/Excel sin recalcular notas. | Generadores `ReportePdfOpenPdf` y `ReporteExcelPoi` (no son el Proxy). |
| RF-21 | Observer y Facade | Observer: `PublicadorEventos`, `ObservadorAcademico`, `ObservadorCorreo`. Facade: `NotificacionFacade` | El Facade publica convocatoria/cambio; el Observer envía correo con idempotencia. | `NotificacionCorreoServicio` persiste el intento SMTP; no es un segundo observador. |
| RF-24 | Strategy | `EstrategiaEvaluacion`, `EvaluacionIncompletaStrategy`, `EvaluacionAusenciaStrategy`, `EvaluacionCompletaStrategy` | Incompleta usa respuestas registradas; ausencia deja nota 0 en `NO_INICIADA`. | `EvaluacionFacade.evaluarPendientes` y el State (`marcarIncompleta`) coordinan el cierre; 4.1 no pide Facade ni State en este RF. |
| RF-26 | Facade y Proxy | `RetroalimentacionFacade`, `GestionRetroalimentacion`, `RetroalimentacionProxy` | El Proxy restringe registro y consulta; el Facade guarda el texto por intento. | Al guardar se publica un evento (eso es RF-27, no un patrón extra de RF-26). |
| RF-27 | Observer y Facade | Observer: `PublicadorEventos`, `ObservadorAcademico`, `ObservadorCorreo`. Facade: `NotificacionFacade.publicarCalificaciones`; el feedback sale de `RetroalimentacionFacade.guardar` | Avisa calificaciones ya persistidas y retroalimentación; no altera `notaFinal`. | `EvaluacionFacade.publicarCalificaciones` solo delega en `NotificacionFacade`. |

Los tipos en `contrato` y el catálogo en memoria son temporales. JWT se sustituirá por el módulo de usuarios; hoy el actor llega en cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id`, `X-Docente-Id`.

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
| GET | `/api/participaciones` | Listado filtrable (`practica`, `estudiante`, `estado`, `numeroIntento`, `fechaDesde`, `fechaHasta`) |
| POST | `/api/participaciones/{id}/respuestas` | Registrar opción (necesario para RF-13). Body `{ preguntaId, respuestaId }` |
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

```powershell
cd backend
..\tools\apache-maven-3.9.9\bin\mvn.cmd test
```

## Dependencias pendientes de integración

- Módulos de prácticas, casos, preguntas, rúbricas y autorizaciones: hoy `CatalogoAcademicoEnMemoria`.
- JWT / usuarios: actor por cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id`, `X-Docente-Id`.
- Persistencia de producción: `ddl-auto` none + PostgreSQL; falta Flyway.
- SMTP real / Mailhog: las pruebas usan `JavaMailSender` mockeado; no se envían correos reales.
- El módulo de prácticas debe invocar los endpoints de convocatoria/cambio; este módulo no crea prácticas.
- OpenPDF 2.0.3 (MPL-2.0 / LGPL-2.1+): ya estaba en el esqueleto; el equipo no exigió licencia Apache exclusiva. Si eso cambia: PDFBox (Apache 2.0) o HTML+Chrome headless.
