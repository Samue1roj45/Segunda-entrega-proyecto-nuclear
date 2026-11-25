# Ecos de una decisión · Fase 2: arquitectura y contratos

Documento de diseño. Todavía no hay código de esta integración. Lo marcado **[pendiente Unreal]** depende de datos del proyecto de Unreal que no se han podido leer.

## 1. Idea central

El juego no es una página nueva ni un cuestionario web. SOMA sigue siendo la fuente de verdad (identidad, permisos, intentos, notas). Unreal solo **ejecuta la experiencia** y le informa a SOMA lo que pasó, siempre por la API, nunca por la base de datos.

```
Navegador (Angular)  ──HTTPS──►  Backend SOMA (Spring Boot) ──► MySQL
      │  WebRTC                      ▲         ▲
      ▼                              │ HTTPS   │ HTTPS saliente
Pixel Streaming (señalización) ◄─────┘         │ (cola de trabajos)
      ▲                                        │
      └──────  Agente local (PC) ──────────────┘
                   │ lanza, vigila y cierra
                   ▼
            Ecos.exe (build empaquetado, una instancia por estudiante)
                   │ HTTPS con token de sesión de juego
                   └────────────► Backend SOMA  (/api/v1/unreal/**)
```

## 2. Reutilización del modelo existente

| Concepto del prompt | Se mapea a |
|---|---|
| Simulación Ecos | un `casos_estudio` publicado con sus 6 preguntas y su rúbrica |
| SimulationAssignment | `practicas` (docente, fechas, lugar) + `autorizaciones` (quién puede jugar) |
| GameAttempt | `participacion` ampliada con columnas nuevas (versión de juego, versión de rúbrica, acto, progreso, tiempo activo) |
| GameAnswer | `respuesta_seleccionada` (más puntos otorgados y estado: correcta, incorrecta, incompleta) |
| Evaluación | `resultado` + `nota_oficial` (ya existen) |
| Informes PDF/Excel | `ReportePdfOpenPdf` y `ReporteExcelPoi` ampliados |
| Auditoría | `auditoria` existente |

Intentos extra: `autorizaciones_repeticion` ya implementa "autorizar nuevos intentos".

## 3. Tablas nuevas (migraciones desde V46, reversibles, sin tocar datos)

| Tabla | Para qué | Claves y restricciones clave |
|---|---|---|
| `ecos_version_juego` | versión del build y de la rúbrica vigentes | `version` única; `estado` ACTIVA/RETIRADA |
| `ecos_decision` | las 8 decisiones registradas | FK a `participacion`; única `(participacion_id, evento_id)` para idempotencia; `secuencia` creciente |
| `ecos_checkpoint` | snapshot versionado del estado | FK a `participacion`; `version_estado`; JSON limitado en tamaño; se conserva solo el último válido y los de cambio de acto |
| `ecos_criterio_resultado` | desempeño por cada uno de los 8 criterios | FK a `resultado`; única `(resultado_id, criterio_id)` |
| `ecos_servidor` | PCs que ejecutan el juego | `nombre` único; `max_instancias`; `ultimo_latido` |
| `ecos_sesion_juego` | una ejecución de Unreal por estudiante | FK a `participacion` y `ecos_servidor`; `estado`; `ultimo_latido`; hash del token (nunca el token) |
| `ecos_cola` | turnos de espera | única solicitud activa por `participacion_id`; `expira_en` |

Políticas: las tablas de sesión y cola se depuran a los 30 días; decisiones, checkpoints finales y resultados se conservan con el intento. No se guarda información clínica real; `serializedState` solo trae lo necesario para reanudar (acto, objetivo, inventario de pistas, banderas narrativas), nunca posición del jugador ni datos del mundo.

## 4. Credenciales (nada de contraseñas dentro del juego)

1. El estudiante inicia sesión en SOMA como hoy.
2. Al pedir jugar, el backend crea una `ecos_sesion_juego` y emite un **token de sesión de juego**: JWT corto (10 min para conectarse, luego renovable por latido), con `sub`=estudiante, `participacion`, `sesion`, `version`, `aud`=`unreal`, y un identificador `jti` guardado como hash. Es distinto del token web y solo vale para `/api/v1/unreal/**`.
3. El token viaja al reproductor de Pixel Streaming y se entrega al juego por el canal de datos del reproductor (no por la URL, para que no quede en historiales ni logs).
4. Se revoca al terminar la sesión, al expirar el latido o si el docente anula el intento.
5. El agente local usa una **credencial de servicio** propia (clave por servidor, guardada como hash, rotable), con permisos solo sobre `/api/v1/internal/game-server/**`.
6. El acceso al reproductor lo valida el backend en cada conexión de señalización; un estudiante no puede entrar a una instancia ajena cambiando un identificador.

## 5. API

Convención: versión en la ruta, JSON, errores con el formato que ya usa `ManejadorErroresApi`, validación con Bean Validation.

**Estudiante** (JWT web, permiso `PARTICIPAR_EN_CASOS`)
- `GET /api/v1/ecos/disponibilidad`, `GET /api/v1/ecos/asignaciones`
- `POST /api/v1/ecos/intentos` (nuevo), `GET /api/v1/ecos/intentos`, `GET /api/v1/ecos/intentos/{id}`
- `POST /api/v1/ecos/intentos/{id}/jugar` (inicia o continúa: devuelve estado de cola o credencial de conexión)
- `GET /api/v1/ecos/cola`, `DELETE /api/v1/ecos/cola`
- `GET /api/v1/ecos/intentos/{id}/resultado`

**Juego** (token de sesión de juego, nunca JWT web)
- `POST /api/v1/unreal/sesion/validar`, `POST /api/v1/unreal/sesion/latido`, `POST /api/v1/unreal/sesion/desconectar`
- `GET /api/v1/unreal/intento/estado`
- `POST /api/v1/unreal/intento/checkpoint`, `/decisiones`, `/respuestas`, `/eventos`, `/finalizar`

**Docente** (permiso `CONSULTAR_PARTICIPACIONES`; solo sus sesiones)
- `GET /api/v1/docente/ecos/panel`, `/estudiantes`, `/estudiantes/{id}`, `/intentos/{id}`, `/analitica`
- `POST`/`PATCH /api/v1/docente/ecos/asignaciones` (reusa `practicas`)
- Informes: PDF individual, Excel grupal y CSV desde los generadores existentes

**Agente** (credencial de servicio)
- `POST /api/v1/internal/game-server/registrar`, `/latido`, `/sesion/lista`, `/sesion/fallida`, `/sesion/cerrada`
- `GET /api/v1/internal/game-server/trabajos` (el agente consulta; el backend nunca le abre conexiones entrantes)

## 6. Contrato de comunicación con Unreal

Todo evento lleva `eventId` (UUID generado en el juego), `sessionId`, `secuencia` creciente y `ocurridoEn`. Reenviar el mismo `eventId` devuelve `200` con el mismo resultado y no duplica nada (idempotencia). Un evento con sesión vencida o secuencia imposible se rechaza (`409`/`410`) y se audita.

```json
// POST /api/v1/unreal/intento/decisiones
{ "eventId": "d3b0…", "secuencia": 12, "acto": 3, "decisionId": "<id-del-juego>", "opcionId": "<id-del-juego>", "ocurridoEn": "2026-11-25T22:10:00Z" }

// POST /api/v1/unreal/intento/respuestas
{ "eventId": "a91c…", "secuencia": 14, "preguntaId": "<id-del-juego>", "respuestaId": "<id-del-juego>", "ocurridoEn": "…" }

// POST /api/v1/unreal/intento/finalizar
{ "eventId": "f007…", "secuencia": 31, "versionJuego": "…", "versionRubrica": "…",
  "segundosActivos": 0, "actosCompletados": [1,2,3,4,5,6], "desenlaceId": "<id-del-juego>" }
```

Los valores van vacíos o con marcadores porque los identificadores reales **[pendiente Unreal]**. El cliente **no envía nota**: envía ids; el backend calcula.

## 7. Evaluación: el backend es la fuente de verdad

- Cada intento guarda `versionRubrica` y `versionJuego`; la fórmula queda ligada al intento.
- Regla documentada en el informe del juego (a confirmar con el código y la rúbrica definitiva): 6 preguntas suman 75 puntos, 8 decisiones suman 25, una respuesta incompleta vale el 40 % de su puntaje, nota = puntaje / 20, y el desenlace narrativo **no** cambia la nota. Los puntos por ítem **[pendiente Unreal / rúbrica]**: no se inventan; se cargan desde un archivo de rúbrica versionado.
- El backend recalcula con lo sincronizado; si el juego mostró otra nota, se registra la discrepancia en `auditoria` y manda la del backend.
- Ajustes manuales del docente: se guarda el resultado original, quién, cuándo y por qué (`auditoria` + campo de ajuste); nunca se sobrescribe.

## 8. Cola y capacidad

- Capacidad inicial: **1 instancia** por servidor (`max_instancias=1`); subir ese número solo tras medir CPU, RAM, VRAM, codificación de video y red.
- Reserva atómica: `UPDATE ecos_servidor SET instancias_activas = instancias_activas + 1 WHERE id = ? AND instancias_activas < max_instancias` y se continúa solo si afectó una fila.
- Estados de cola/sesión: EN_ESPERA, ASIGNANDO, INICIANDO, LISTO, JUGANDO, RECONECTANDO, FINALIZANDO, COMPLETADO, CANCELADO, ERROR, SERVIDOR_NO_DISPONIBLE.
- Posición real (por orden de solicitud); sin tiempos inventados: solo se muestra una estimación si hay suficientes sesiones completadas y siempre rotulada "estimada".
- Reserva con vencimiento, una solicitud activa por estudiante, tarea periódica que libera sesiones sin latido (inactividad configurable) y recupera el estado al reiniciar el backend.
- Si una instancia se cae, el progreso ya sincronizado no se pierde: el estudiante reanuda desde el último checkpoint válido.

## 9. Agente local

- Proceso pequeño en el PC que **solo hace conexiones salientes** HTTPS al backend (no abre puertos a Internet).
- Acciones fijas: registrar servidor, latido, tomar trabajos, lanzar el ejecutable en una ruta configurada y verificada, vigilar el proceso, informar errores, cerrar y liberar.
- Los parámetros de lanzamiento los arma el agente a partir de identificadores internos (puerto asignado, id de sesión); nada que escriba un estudiante llega a la línea de comandos.
- Un puerto de streaming distinto por instancia.
- Configuración (ruta del ejecutable, capacidad, URL y clave de servicio) en un archivo de ejemplo `.env.example`, sin credenciales reales.

## 10. Pixel Streaming y red

- Se usa la infraestructura oficial de Epic compatible con la versión de Unreal del proyecto **[pendiente Unreal]**.
- WebRTC necesita STUN y, detrás de NAT o en redes de universidades, TURN. Un TURN en la nube puede generar costo; **no se contrata nada sin autorización**.
- El reproductor se integra como pantalla dentro de la zona del estudiante, con pantalla completa y manejo de foco de teclado y ratón.
- Plan del equipo: desplegar el juego en la nube durante la presentación y bajarlo si no ganan. El diseño lo permite: se cambia el agente del PC por uno en una máquina con GPU sin tocar backend ni frontend. Ese costo es de GPU por hora y lo decide el equipo.

## 11. Frontend

Mínimo necesario y con el diseño actual: tarjeta "Ecos de una decisión" en la zona del estudiante (disponibilidad, jugar/continuar, cola, historial, informe) con el reproductor; en la del docente, panel, lista de estudiantes con filtros, detalle por intento y criterio, y descarga de informes. Se reutilizan los componentes de gráficas y el panel de estadísticas ya hecho. Sin datos ficticios presentados como reales.

## 12. Módulo de Unreal

Un subsistema único (`GameInstanceSubsystem`, C++ con Blueprints expuestos) que centraliza HTTP/JSON, reintentos limitados, timeouts, cola local de eventos pendientes e idempotencia por `eventId`, sin bloquear el hilo del juego y sin claves permanentes en el ejecutable. Se implementa cuando se pueda abrir el proyecto.

## 13. Orden de trabajo propuesto

Independientes de Unreal (se pueden hacer ya): migraciones, API del estudiante/docente, intentos y evaluación con rúbrica cargable, tokens de juego, cola y agente simulado, frontend, pruebas con un "juego simulado" que reproduce el contrato. Dependientes de Unreal: ids reales, valores de la rúbrica, módulo en Unreal, Pixel Streaming real y prueba de partida completa.
