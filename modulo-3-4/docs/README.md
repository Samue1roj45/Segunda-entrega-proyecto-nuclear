# Módulos 3 y 4 · Programación de sesiones y participación

Responsable: Peter Manolo Carvajal Lugo · Ramas `develop/feature/peter/modulo3`, `develop/feature/peter/modulo4` y
`develop/feature/peter/tests` (unidas en `main` dentro de `modulo-3-4/backend/`).

| Módulo | Requisitos |
|---|---|
| 3. Asociación académica, programación y convocatorias | RF-04, RF-18, RF-19, RF-20 |
| 4. Participación y control de sesiones | RF-10, RF-11, RF-22, RF-23, RF-25, RF-28 |

El registro del inicio y fin de cada intento (RF-12) es de los módulos 5 y 6: este módulo le aplica la regla de
ingreso (RF-22) y los intentos permitidos (RF-25), y registra las respuestas por el puerto del módulo 5.

## Estructura (`modulo-3-4/backend/src/main/java/co/edu/humboldt/plataformacasos/sesiones`)

| Paquete | Contenido |
|---|---|
| `web` | Controladores REST: `SesionControlador`, `ParticipacionEstudianteControlador`, `RepeticionControlador`, `HerramientaControlador` |
| `fachada` | `FachadaSesiones` (Facade): el docente agenda, modifica y gestiona sesiones solo a través de ella |
| `servicio`, `servicio/validacion`, `servicio/repeticion` | Casos de uso y cadenas de validación (Chain of Responsibility) |
| `estado`, `politica`, `proxy`, `evento` | State (RF-19), Strategy (RF-22), Proxy (RF-10, RF-11, RF-23) y Observer (RF-11, eventos hacia otros módulos) |
| `repositorio` | Acceso a datos con JdbcTemplate (sin procedimientos almacenados) y el gateway de la participación |
| `integracion` | `ConsultaSesiones`: contrato de solo lectura para los demás módulos |
| `tareas`, `configuracion` | Cierre automático (RF-19), expiración de intentos (RF-23) y elección de la política de ingreso |
| `dominio`, `dto` | Objetos del dominio (`Practica`, `Autorizacion`, `Herramienta`, `SesionProgramada` con Builder) y datos del API |

Pruebas en `modulo-3-4/backend/src/test/java/.../sesiones/modulo3` y `modulo4`, separadas en `positivas` y
`negativas` (60 casos CP-RFxx-yy). `soporte/Datos` tiene los datos de apoyo.

## Base de datos

Migraciones Flyway `V30` a `V39` en `modulo-3-4/backend/src/main/resources/db/migration`:

| Tabla | Para qué |
|---|---|
| `practicas` | Sesiones: caso (módulo 2), docente (módulo 1), condiciones, estado `SIN_INICIAR` / `EN_CURSO` / `FINALIZADO`, inicio y cierre reales |
| `autorizaciones` | Estudiantes convocados (`ACTIVA` / `RETIRADA`) y el **hash** BCrypt de su código de acceso |
| `autorizaciones_repeticion` | Cada fila concede un intento extra (RF-25) |
| `herramientas_caso` | Herramientas profesionales de cada caso (RF-28) |

Las reglas de negocio están en Java (Chain, State, Strategy); la base solo garantiza la integridad. El intento
del estudiante y sus respuestas se guardan en las tablas del módulo 5 (`participacion`, `respuesta_seleccionada`).
Semilla de prueba: `db/semilla/V35_1__Sesion_prueba` (sesión SIN_INICIAR con Ana y Luis convocados).

## API

El usuario es el del token del módulo 1 (`Authorization: Bearer <token>`); ya no se usan las cabeceras
`X-Docente-Id` ni `X-Estudiante-Id`.

| Método y ruta | Quién (permiso) | RF |
|---|---|---|
| `POST /api/sesiones` | Docente (`PROGRAMAR_SESIONES`) | RF-04, RF-18: agenda y convoca; cada estudiante recibe su código por correo |
| `GET /api/sesiones?estado=`, `GET /api/sesiones/{id}` | Docente | RF-18 |
| `PUT /api/sesiones/{id}` | Docente | RF-18: cambia condiciones; los convocados reciben el aviso (RF-21) |
| `POST /api/sesiones/{id}/iniciar`, `/finalizar` | Docente | RF-19 (al finalizar, el módulo 5 evalúa los pendientes, RF-24) |
| `GET/POST/DELETE /api/sesiones/{id}/estudiantes[/{idEstudiante}]` | Docente | RF-04 |
| `POST /api/autorizaciones/{id}/repeticiones`, `GET .../intentos` | Docente creador del caso (`AUTORIZAR_REPETICION`) | RF-25 |
| `GET /api/autorizaciones` | Estudiante (`PARTICIPAR_EN_CASOS`) | RF-20: sus convocatorias, con intentos usados y permitidos |
| `GET /api/autorizaciones/{id}/ingreso` | Estudiante | RF-22 |
| `GET /api/participaciones/{id}/contexto` | Estudiante dueño | RF-10 (sin clave: contrato `contenidoSinClaves` del módulo 2) |
| `PUT /api/participaciones/{id}/preguntas/{idPregunta}/respuesta` | Estudiante dueño | RF-10, RF-11 |
| `GET /api/participaciones/{id}/progreso`, `/tiempo` | Estudiante dueño | RF-11, RF-23 |
| `GET /api/participaciones/{id}/herramientas` | Estudiante dueño, intento en curso | RF-28 |
| `GET/POST/DELETE /api/casos/{idCaso}/herramientas[/{id}]` | Docente creador del caso (`GESTIONAR_CASOS`) | RF-28 |

Iniciar y finalizar el intento: `POST /api/participaciones/iniciar` y `POST /api/participaciones/{id}/finalizar`
(módulos 5 y 6, RF-12). El ingreso del estudiante es el login del módulo 1 con su correo y su código (RF-01 + RF-20).

## Comunicación con los demás módulos

| Con | Cómo |
|---|---|
| Módulo 1 | Implementa su puerto `VerificadorCodigoAutorizacion` (`VerificadorCodigoAcceso`, RF-20); usa `ServicioAutorizacion` y `UsuarioRepositorio` |
| Módulo 2 | `ConsultaCasosProgramables` (caso publicado y tiempo estimado al agendar) y `ContenidoCasoInterno` (contenido sin clave, docente creador) |
| Módulo 5 | Puerto `RegistroParticipaciones` (intentos, respuestas, cierre por tiempo). El módulo 5 consulta `ConsultaSesiones` y la regla de ingreso (`ServicioIngresoTardio`) a través de la aplicación |
| Módulo 6 | Eventos `EstudianteConvocadoEvento`, `SesionModificadaEvento` y `SesionFinalizadaEvento` (Observer) |

## Cambios hechos al integrar

- Paquete `com.example.demo` → `co.edu.humboldt.plataformacasos.sesiones` y nombres en español como el resto del
  equipo (`*Controller` → `*Controlador`, `*Repository` → `*Repositorio`, `*Service` → `Servicio*`, `Facade` →
  `FachadaSesiones`).
- Los procedimientos almacenados y triggers que el código esperaba nunca se subieron: los repositorios se
  reescribieron con SQL simple sobre las tablas `V30` a `V32` y las reglas pasaron a Java.
- Se quitó el código que duplicaba a otros módulos: usuarios (módulo 1), casos (módulo 2), resultados,
  retroalimentación, reportes, notificaciones y registro de tiempos (módulos 5 y 6), `/api/acceso/estudiante`
  (login del módulo 1) y los `model/fila` sin uso. Los errores usan las excepciones y el formato del módulo 1.
- Reglas nuevas: el tiempo máximo cabe en el horario y no supera el estimado del caso (RF-18), una sola
  repetición por estudiante (RF-25, máximo dos intentos), no se responde con el tiempo agotado (RF-23) y la
  regla de ingreso cuenta desde el inicio real de la sesión.
- RF-28: las herramientas ahora se guardan por caso en `herramientas_caso` (antes se leían de un documento DSL
  que el módulo 2 no guarda).
