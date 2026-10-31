# Informe V6 — Reestructuración de la experiencia de juego

**Fecha:** 2026-06-03  
**Alcance:** Solo frontend del simulador (`frontend/src/app/estudiante/simulacion-play/`)

## Resumen

V6 convierte hospital y comisaría en escenarios explorables con zonas, colisiones, cámara que sigue al jugador, entrevistas ramificadas y visor de documentos con registro manual en expediente. Se corrige el bug de retorno al spawn al cerrar diálogos.

## Entregables

| # | Entregable | Estado |
|---|------------|--------|
| 1 | Corrección bug spawn | ✅ |
| 2 | Layout hospital por zonas | ✅ |
| 3 | Layout comisaría por zonas | ✅ |
| 4 | Conversaciones ramificadas (entrevistas) | ✅ |
| 5 | Documentos interactivos (visor) | ✅ |
| 6 | Fondos visuales (zonas + mobiliario procedural) | ✅ |
| 7 | Lista de archivos modificados | ✅ (abajo) |
| 8 | Build `--configuration=docker` | ✅ |
| 9 | Sin cambios en MemoriasDelParque / backend | ✅ |

## Bug spawn — causa y fix

**Causa:** `limpiarDialogo()` llamaba a `scheduleMountEngine()`, que en un engine ya montado ejecutaba `loadScene()` y reseteaba `playerStart`.

**Fix:**
- Eliminar `scheduleMountEngine()` de `limpiarDialogo()`.
- En `mountEngine()`, si la escena no cambió, solo `updateSceneEntities()` + `resize()`.
- `loadScene(scene, { preservePlayer })` para transiciones que lo requieran.

## Mapas explorables

### Hospital (1280×800)

| Zona | NPCs / objetos |
|------|----------------|
| Recepción | Guardia |
| Sala de espera | Madre, Hermano |
| Pasillo principal | (conexión) |
| Enfermería | Enfermería |
| UCI | Médico |
| Archivo clínico | Historia clínica, EPICEE |

Paredes, puertas (huecos en obstáculos), mobiliario (escritorios, sillas, camas, archivadores).

### Comisaría (1280×800)

| Zona | NPCs / objetos |
|------|----------------|
| Recepción | Secretaría, Comisario |
| Trabajo Social | Trabajador social |
| Protección | Protección, Víctima |
| Archivo | Expediente, Denuncias, Reportes, Ruta |

## Entrevistas ramificadas

- `InterviewService` + `interview-topics.catalog.ts` con temas por NPC (hechos, antecedentes, relación, menor, etc.).
- Requisitos: temas previos, mínimo de pistas, pistas específicas.
- UI: `InterviewPanelComponent` → elige tema → diálogo línea a línea → «Registrar declaración».
- Volver al menú de entrevista sin reiniciar posición.

## Documentos

- `DocumentViewerComponent`: lectura completa antes de registrar.
- Botón «Registrar en expediente»; no auto-registro al abrir.
- Sigue usando tiers de `hospital.investigation.ts` / `comisaria.investigation.ts` vía `ConversationService`.

## Motor canvas

- Cámara centrada en el jugador (`VIEW_SCALE = 1.65`).
- `drawZones`, `drawFurniture`, paredes desde `obstacles`.
- Fondo SVG existente como capa semitransparente bajo zonas.

## Archivos modificados / creados

### Modificados
- `frontend/src/app/estudiante/simulacion-play/simulacion-play.component.ts`
- `frontend/src/app/estudiante/simulacion-play/simulacion-play.component.html`
- `frontend/src/app/estudiante/simulacion-play/data/scene-types.ts`
- `frontend/src/app/estudiante/simulacion-play/data/hospital-scene.content.ts`
- `frontend/src/app/estudiante/simulacion-play/data/comisaria-scene.content.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/scene-renderer.ts`
- `frontend/src/app/estudiante/simulacion-play/presentation/inworld-dialogue.component.ts`

### Creados
- `frontend/src/app/estudiante/simulacion-play/data/investigation/interview-topics.catalog.ts`
- `frontend/src/app/estudiante/simulacion-play/services/interview.service.ts`
- `frontend/src/app/estudiante/simulacion-play/presentation/interview-panel.component.ts`
- `frontend/src/app/estudiante/simulacion-play/presentation/document-viewer.component.ts`
- `docs/INFORME_V6_IMPLEMENTACION.md`

## Build

```bash
cd frontend
npm run build -- --configuration=docker
```

Resultado: **exitoso** (2026-06-03).

## No modificado (según restricciones)

- MemoriasDelParque-master
- Backend, auth, docente, DecisionBridgeService, APIs
- `ConversationService` (contrato tiers para documentos)
