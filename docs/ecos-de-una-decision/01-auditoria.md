# Ecos de una decisión · Fase 1: auditoría de SOMA

Fecha: 25-nov-2026. Solo se leyó el repositorio; no se modificó código. Lo que no se pudo verificar está marcado como **pendiente**.

## 1. Tecnologías reales de la plataforma

| Aspecto | Hallazgo |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2 (`spring-boot-starter-webmvc`), un solo despliegue en `backend/` |
| Arquitectura | Monolito modular por paquetes: `usuarios`, `autenticacion`, `autorizacion`, `casos`, `rubricas`, `sesiones`, `evaluacion`, `avisos`, `preferencias`, `integracion`. Los módulos se hablan por puertos (`CatalogoAcademico`, `ActorActualResolver`…) y patrones documentados en `docs/PATRONES.md` |
| Base de datos | MySQL 8 (`plataforma_casos`); H2 en modo MySQL para las pruebas |
| ORM / migraciones | Spring Data JPA + Hibernate; Flyway, migraciones `V1` a `V45` (`V46` en adelante libres; `V40`–`V49` se habían reservado a los módulos 5 y 6) |
| Autenticación | JWT HS256 emitido por `EmisorToken` (reclamos: `sub`=id usuario, correo, rol, permisos). Personal con correo y clave; estudiante con correo y código de autorización de la sesión. Bloqueo tras 5 intentos fallidos |
| Roles | `ESTUDIANTE`, `DOCENTE`, `ADMINISTRADOR`. Permisos finos por usuario (`GESTIONAR_CASOS`, `PROGRAMAR_SESIONES`, `CONSULTAR_PARTICIPACIONES`, `DESCARGAR_REPORTES`, `PARTICIPAR_EN_CASOS`, `ELIMINAR_CASOS`, etc.), verificados con `@RequierePermiso` en el backend |
| Seguridad HTTP | Todo requiere autenticación salvo `POST /api/auth/**` y `GET /api/salud` |
| Frontend | Angular 21 (standalone, signals), Vitest, Playwright con servidor simulado; guardias por permiso en las rutas |
| Reportes | PDF con OpenPDF y Excel con Apache POI ya existentes (`/api/reportes/evaluacion.pdf` y `.xlsx`) |
| Correo y avisos | Correo asíncrono (Mailpit en desarrollo), campana de avisos, auditoría y bloqueo de ingreso |
| Despliegue | `docker-compose.yml` con `db` (MySQL), `app` (backend) y `correo` (Mailpit); CI en GitHub Actions |

## 2. Modelo de datos que se puede reutilizar

| Necesidad de Ecos | Ya existe | Tabla |
|---|---|---|
| Estudiante y docente | Sí | `usuario`, `usuario_permiso` |
| El juego como "caso" con preguntas y rúbrica | Sí | `casos_estudio`, `escenarios`, `preguntas`, `respuestas`, `rubricas`, `criterios_evaluacion` |
| Asignación del docente (fechas, lugar, tiempo) | Sí | `practicas` (sesión) |
| Estudiante autorizado en una asignación | Sí | `autorizaciones` (+ `autorizaciones_repeticion` para intentos extra) |
| Intento | Sí | `participacion` (`numero_intento`, `inicio`, `fin`, `tiempo_usado_seg`, `estado`) |
| Respuestas elegidas | Sí | `respuesta_seleccionada` (única por participación y pregunta) |
| Resultado y nota | Sí | `resultado`, `nota_oficial` |
| Retroalimentación docente | Sí | `retroalimentacion`, `notificacion` |
| Auditoría y avisos | Sí | `auditoria`, `aviso` |
| Decisiones narrativas (8) | **No** | — |
| Checkpoints / snapshot de partida | **No** | — |
| Desempeño por 8 criterios del juego | **No** (hay criterios de rúbrica por caso, sin resultado por intento) | — |
| Servidores de juego, instancias, cola | **No** | — |
| Versión del juego y de la rúbrica por intento | **No** | — |

Conclusión: la forma correcta es **extender** `participacion` como "intento de Ecos" y añadir tablas nuevas solo para lo que falta, sin duplicar usuarios, cursos ni notas.

## 3. Lo que no se pudo auditar

El proyecto de Unreal Engine no está disponible en este entorno (no hay `.uproject` en el repositorio ni acceso a la carpeta del equipo). Por eso quedan **pendientes de verificar contra el proyecto real**:

- Versión exacta de Unreal Engine y del plugin de Pixel Streaming (1 o 2).
- `GameInstance`, `GameMode`, `PlayerController` y sistema de guardado.
- Identificadores reales de las 6 preguntas, las 8 decisiones, los 6 actos y los 8 criterios.
- Valor individual de cada pregunta y decisión. El informe habla de 75 puntos para las preguntas y 25 para las decisiones, pero **no hay valores por ítem y no se inventan**.
- Regla exacta de "respuesta incompleta" (40 % del valor) y qué la produce.
- Cómo se cierra una partida hoy (informe de evaluación) y si ya existe algún HTTP desde el juego.

Tampoco hay datos del hardware del computador (GPU, RAM, VRAM), así que no se fija capacidad mayor a una instancia.

## 4. Diferencias entre el prompt y la plataforma

- El prompt usa nombres tipo `/api/v1/game/...`; la plataforma usa `/api/...` en español y sin versión. Se propone `/api/v1/ecos/...` para lo nuevo y `/api/v1/unreal/...` para el juego, sin tocar lo existente.
- "Curso/grupo" no existe como entidad en SOMA: hoy el docente convoca estudiantes a una sesión (`practicas` + `autorizaciones`). La asignación a grupo se resuelve con convocatorias; crear cursos queda como decisión del equipo.
- Los estudiantes no se pueden crear desde la interfaz (solo por semilla o SQL); sin eso no hay "estudiantes asignados" reales.
- El juego en la nube (plan del equipo para la presentación) no cambia el diseño: el agente local es el "servidor de ejecución" y se reemplaza luego por uno en la nube.
