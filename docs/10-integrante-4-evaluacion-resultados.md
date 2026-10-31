# Integrante 4 — Evaluación y resultados (esqueleto Spring Boot)

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

1. Dos intentos por autorización. Historial independiente. La ausencia (nota 0) no reabre el mismo intento ni bloquea el segundo.
2. Notificaciones por correo Spring Boot.
3. `nota = (peso_obtenido / peso_total) * rubrica.escala_maxima` (100 si no hay escala).
4. RN17: no iniciar después de la mitad del intervalo de la práctica.

## Estado

- RF-12 y RF-15: implementados.
- RF-13, RF-14 y RF-24: implementados (nota con rúbrica, consulta, incompletos y ausencias).
- RF-16: implementado (PDF OpenPDF 2.0.3 y Excel Apache POI 5.4.1).
- RF-21, RF-26 y RF-27: implementados (correo Spring Mail, retroalimentación por intento, publicación de notas).

Los tipos en `contrato` y el catálogo en memoria son temporales. JWT se sustituirá por el módulo de usuarios; hoy el actor llega en cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id`, `X-Docente-Id`.

## RF-13 / RF-14 / RF-24

- Fórmula: `(peso_obtenido / peso_total) * rubrica.escala_maxima`. Si no hay rúbrica, escala = 100.
- Un `Resultado` por participación (intento). Un reintento crea otra participación y otro resultado; no se reescribe el anterior.
- **Incompleta:** participación `EN_CURSO` al evaluar pendientes → estado `INCOMPLETA` y nota con las respuestas registradas (preguntas sin respuesta no suman peso).
- **Ausencia:** autorización sin intento iniciado → participación `NO_INICIADA` con nota 0. Ese intento no se puede iniciar después; el estudiante puede abrir el segundo intento.
- Solo el estudiante dueño responde y finaliza. `calcular` no reescribe una nota ya persistida.
- Nota oficial: el docente elige el intento (`POST /api/resultados/nota-oficial`). No se altera `notaFinal` de cada intento.
- Consulta: estudiante solo sus resultados; docente solo prácticas con `docenteId` propio.

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
