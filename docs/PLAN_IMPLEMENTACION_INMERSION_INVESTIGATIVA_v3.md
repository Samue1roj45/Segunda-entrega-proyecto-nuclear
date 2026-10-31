# Plan técnico de implementación — Experiencia de investigación inmersiva (v3)

**Estado:** PLANIFICACIÓN — **no iniciar código** hasta autorización explícita (`Inicia I0`, etc.)  
**Fecha:** 2026-06-03  
**Alcance:** Solo frontend (`simulacion-play`, `resultado`, assets públicos de simulación)  
**Documento base:** `PLAN_REDISEÑO_INMERSION_MAXIMA.md` (v2), ampliado con reglas de evaluación y cierre académico

---

## 0. Regla fundamental (no negociable)

### Durante la simulación (desde spawn hasta antes de `finalizar()`)

**PROHIBIDO** mostrar o insinuar evaluación académica:

- Correcto / incorrecto / buena / mala respuesta  
- Retroalimentación pedagógica de opciones  
- Explicación de la opción “recomendada” del backend  
- Porcentajes de “acierto” en minijuegos (verde/amarillo, “orden correcto”)  
- Bloques tipo `narrativeFeedback` que juzgan la decisión (“eleva el riesgo”, “revisión académica”)  
- Barras `%` de “calidad de intervención” visibles como nota encubierta  

**PERMITIDO** durante el juego:

- Consecuencias narrativas (qué pasa en el mundo)  
- Reacciones de personajes  
- Información nueva / cambios en el caso  
- Tablero **Caso en construcción** (hechos, riesgos, contradicciones — sin calificar)  
- Hipótesis registradas (sin nota)  
- Confirmación de acción profesional (“¿Confirmas esta intervención?”)  

### Después de `finalizar()` — pantalla `/estudiante/resultado/:id`

**Aquí sí** existe retroalimentación académica completa, usando datos del backend (`es_correcta`, `retroalimentacion`, `justificacion`, `respuestas_correctas`) **sin modificar APIs**.

---

## 1. Objetivo de producto

El estudiante debe sentir que **investiga y resuelve un caso**, no que responde un cuestionario.

| Fase mental del estudiante | Cuándo |
|----------------------------|--------|
| “Indago, entrevisto, encuentro contradicciones” | Hospital + Comisaría |
| “Formulo hipótesis y actúo” | Antes de cada decisión |
| “Veo qué pasa en el mundo” | Tras cada decisión |
| “Cierro el expediente del caso” | Síntesis + finalizar |
| “Ahora entiendo qué fue adecuado y qué no” | Solo en resultado |

---

## 2. Flujo de fases (máquina de estados nueva)

Reordenación respecto al código actual: los minijuegos pasan **después** de la decisión de comisaría, no entre hospital y comisaría.

```mermaid
flowchart TB
  START[iniciarParticipacion] --> EXP_H[explore hospital]
  EXP_H --> INV_H{Umbral investigación H}
  INV_H -->|no| EXP_H
  INV_H -->|sí| HYP_H[hypothesis-hospital]
  HYP_H --> DEC_H[decision-hospital]
  DEC_H --> CONF_H[confirm-hospital]
  CONF_H --> API_H[responder silencioso]
  API_H --> CONS_H[consequence-hospital]
  CONS_H --> BRIDGE[Transición diegética a comisaría]
  BRIDGE --> EXP_C[explore comisaria]
  EXP_C --> INV_C{Umbral investigación C}
  INV_C -->|no| EXP_C
  INV_C -->|sí| HYP_C[hypothesis-comisaria]
  HYP_C --> DEC_C[decision-comisaria]
  DEC_C --> CONF_C[confirm-comisaria]
  CONF_C --> API_C[responder silencioso]
  API_C --> CONS_C[consequence-comisaria]
  CONS_C --> MG[minigame-timeline → route → risk]
  MG --> SYN[case-synthesis]
  SYN --> FIN[finalize-confirm]
  FIN --> API_F[finalizar]
  API_F --> RES[/estudiante/resultado/:id]
```

### Tipos `PlayPhase` (reemplazo de `case-continues`)

Eliminar: `case-continues`, `gate-hospital`, `gate-comisaria` (sustituidos por señal diegética + umbral).

Añadir:

| Fase | Canvas | Panel lateral |
|------|--------|---------------|
| `explore` | Sí | Tablero Caso en construcción |
| `hypothesis-hospital` | Sí (pausado) o overlay semitransparente | Tablero |
| `decision-hospital` / `confirm-hospital` | Overlay acción | Tablero |
| `consequence-hospital` | Overlay narrativo | Tablero |
| `hypothesis-comisaria` … | Igual | Tablero acumulativo |
| `minigame-*` | Overlay diegético | Tablero |
| `case-synthesis` | Resumen investigativo | Tablero completo |
| `finalize-confirm` | CTA cerrar expediente | — |

---

## 3. Arquitectura de datos (solo frontend)

### 3.1 Catálogo de pistas (`ClueFragment`)

Cada pista es **atómica** (una idea). Campos mínimos:

```ts
interface ClueFragment {
  id: string;
  sceneId: 'hospital' | 'comisaria';
  sourceType: 'npc' | 'object';
  sourceId: string;
  tier: 1 | 2 | 3;
  boardSection: 'personas' | 'hechos_confirmados' | 'hechos_pendientes' | 'riesgos' | 'evidencias';
  title: string;       // ≤ 8 palabras
  summary: string;     // 1 frase
  factIds?: string[];  // hechos que confirma o deja pendiente
  unlocks?: { npcTier?: Record<string, 2|3>; objectIds?: string[] };
}
```

### 3.2 Contradicciones (`ContradictionPair`)

```ts
interface ContradictionPair {
  id: string;
  sceneId: SceneId;
  label: string;           // título corto en tablero
  clueIds: [string, string]; // dos pistas incompatibles
  narrativeHint?: string;  // solo para síntesis, no evaluativo
}
```

Ejemplos obligatorios (contenido en catálogo):

| Fuente A | Fuente B | Tema |
|----------|----------|------|
| Madre T2 | Hermano T2 | Primera vez vs meses |
| Expediente | Víctima T2 | Denuncia previa sí/no |
| Médico | Guardia | Gravedad / contexto |

### 3.3 Servicios

| Servicio | Responsabilidad |
|----------|-----------------|
| `CaseInvestigationService` | Tablero 6 secciones, hechos confirmados/pendientes, progreso % investigación |
| `ConversationService` | Tier T1–T3, re-diálogo, desbloqueos por pistas |
| `ContradictionService` | Al registrar pista, escanear pares; emitir alerta en tablero |
| `HypothesisService` | Guardar hipótesis H/C (texto + id opción); sin puntuación |
| `CaseClosureService` | Snapshot al síntesis → `sessionStorage` para resultado |
| `InvestigationService` | **Deprecar** lógica plana; delegar o fusionar en `CaseInvestigationService` |
| `DecisionBridgeService` | **Sin cambios de contrato** |
| `NarrativeStateService` | Mantener `delta` interno opcional; **ocultar barras % en HUD** |

### 3.4 Persistencia para pantalla final

Clave: `simulacion-closure:{participacionId}`

```ts
interface CaseClosureSnapshot {
  participacionId: number;
  closedAt: string;
  hypotheses: { hospital: string; comisaria: string };
  cluesCollected: number;
  contradictionsDetected: string[];
  confirmedFacts: string[];
  pendingFacts: string[];
  decisionsNarrative: { hospital: string; comisaria: string }; // actionTitle elegido
  minigamesCompleted: boolean;
}
```

Escrito en `case-synthesis` → leído en `resultado.ts` para **Sección 1**. Borrado en `salir()` del resultado (junto con tokens).

---

## 4. UI — componentes nuevos

| Componente | Función |
|------------|---------|
| `InvestigationBoardComponent` | Panel **“Caso en construcción”** con 6 secciones colapsables |
| `HypothesisPanelComponent` | 5 hipótesis + opcional línea libre; botón “Registrar hipótesis” |
| `CaseSynthesisPanelComponent` | Línea de tiempo inferida, roles, contradicciones abiertas, hipótesis (sin corrección académica) |
| `FinalizeParticipationPanelComponent` | “Cerrar expediente y ver retroalimentación académica” → `finalizar()` |

Reutilizar: `InworldDialogueComponent`, motor canvas, minijuegos (re-skin narrativo en I6).

---

## 5. Integración académica (contratos intactos)

| Momento | API | UI visible |
|---------|-----|------------|
| Confirmar decisión hospital | `SimulacionService.responder(partId, pregunta_id, respuesta_id)` | Solo “Registrando…” / “Intervención registrada en expediente” (sin ✔/✘) |
| Confirmar decisión comisaría | Idem | Idem |
| Cerrar caso | `SimulacionService.finalizar(partId)` | Pantalla síntesis → confirmación |
| Ver nota y retroalimentación | `obtenerResultado(resultado_id)` | Solo en `/estudiante/resultado/:id` |

**No usar** `es_correcta` ni `retroalimentacion` del caso cargado en `participacion.caso` durante `simulacion-play`.

Mapeo decisión → backend: sin cambios (`DecisionBridgeService` + `decision-mapping.config.ts`).

---

## 6. Pantalla final (`resultado.ts`) — rediseño

### Sección 1 — Resumen de tu intervención

- Datos de `CaseClosureSnapshot` (investigación vivida).  
- Frase de cierre: “Completaste la investigación del caso [nombre].”

### Sección 2 — Decisiones tomadas

Filtrar `detalle_preguntas` por escenario usando `participacion.caso.escenarios` + `SCENE_BACKEND_MATCHERS` (misma lógica que bridge):

| Bloque | Contenido |
|--------|-----------|
| **Hospital** | `actionTitle` del snapshot + enunciado académico |
| Si `es_correcta` | Encabezado **✔ Buena decisión** + `retroalimentacion` + `justificacion` |
| Si no | **⚠ Oportunidad de mejora** + riesgos + subbloque **✔ Opción recomendada** (`respuestas_correctas[0]`) |

### Sección 3 — Análisis final (generado en frontend)

Reglas sobre snapshot + resultado API (sin nuevo endpoint):

| Fortalezas (ejemplos) | Condición aproximada |
|-----------------------|----------------------|
| Capacidad investigativa | ≥ N pistas y ≥ 2 fuentes T2 |
| Detección de contradicciones | ≥ 2 contradicciones en snapshot |
| Protección de derechos | decisión comisaría correcta |
| Trabajo interdisciplinario | decisión hospital correcta |

| Áreas de mejora | Condición |
|-----------------|-----------|
| Validación de fuentes | pocas pistas de documentos |
| Detección de contradicciones | 0 contradicciones |
| Evaluación del riesgo | hipótesis “baja” con decisión incorrecta |
| Rutas institucionales | alguna decisión incorrecta |

Mantener hero con nota (backend) y stats — son aceptables **solo post-finalizar**.

### Corrección conocida

- Evitar `localStorage.clear()` antes de leer snapshot; limpiar solo claves de sesión estudiante.

---

## 7. Contenido narrativo — principios de reescritura (I0)

### Hospital (5 NPC + objetos actuales, sin ampliar cast)

| NPC | T1 | T2 | T3 |
|-----|----|----|-----|
| Madre | Versión emocional, vaga | “Primera vez” | No revelar agresor completo en T1 |
| Hermano | Evita detalles | “Desde hace meses” | Contradice madre |
| Médico | Solo clínico | Lesiones, protocolo | Sin dictaminar culpabilidad |
| Víctima | Miedo, fragmentos | Versión propia | No cerrar denuncia en T1 |
| Guardia | Rumor | Escuchó amenazas | Pieza periférica |

Objetos: expediente, protocolo EPICEE, historia clínica — cada uno **1–2 pistas** que pueden confirmar o contradecir testimonios.

### Comisaría

Misma lógica; nuevas contradicciones archivo vs testimonio oral.

### Decisiones (`decisions.content.ts`)

- Mantener `actionTitle` / `actionSubtitle` / `consequenceLines`.  
- **Eliminar** `narrativeFeedback` del template de consecuencia (mover texto evaluativo a plantillas solo usadas en `resultado` si hace falta, o confiar en backend).  
- Mantener `delta` solo si se usa de forma invisible; no mostrar barras.

### Minijuegos (I6)

- Botón: “Entregar al equipo” / “Continuar caso” — no “Validar secuencia”.  
- Al enviar: mensaje **narrativo neutro** (“El equipo archiva tu reconstrucción”) sin ratio ni color ok/error.  
- `completed.emit()` sin usar score para mensajes al usuario (score opcional solo interno, sin UI).

---

## 8. Gating (profundidad investigativa)

Sustituir `EXPLORATION_GATING` (2 NPC + 2 objetos) por umbrales en `CaseInvestigationService`:

### Hospital → hipótesis

| Criterio | Mínimo |
|----------|--------|
| Pistas registradas | 7 |
| Hechos confirmados | 2 |
| Contradicciones | 1 |
| Fuentes con tier ≥ 2 | 3 |
| Categorías en tablero | personas + riesgos + evidencias |

### Comisaría → hipótesis

| Criterio | Mínimo |
|----------|--------|
| Pistas nuevas comisaría | 5 |
| Hechos confirmados (total) | 4 |
| Contradicciones (total) | 2 |
| Documentos inspeccionados | 2 |

Desbloqueo diegético: icono `!` en NPC / objeto interactuable; notificación in-world (“El equipo espera tu valoración”).

---

## 9. Oleadas de implementación

| Oleada | Entregable | Criterio de demo |
|--------|------------|------------------|
| **I0** | Catálogos `data/investigation/*`, reescritura T1 hospital/comisaría sin spoiler | Hablar con todos en T1 sin saber “el caso completo” |
| **I1** | `CaseInvestigationService` + `InvestigationBoardComponent` | Tablero 6 secciones se llena al explorar |
| **I2** | `ContradictionService` + alertas | Madre vs hermano genera fila en Contradicciones |
| **I3** | `ConversationService` + T1–T3 hospital | Re-dialogo desbloquea T2/T3 |
| **I4** | Hipótesis + umbral + decisión/consecuencia H + `responder()` | Flujo H sin texto evaluativo |
| **I5** | Comisaría completa + puente narrativo | Segunda investigación con tablero acumulado |
| **I6** | Minijuegos diegéticos + síntesis + `finalizar()` + `resultado` | Cierre académico solo en resultado |

**Orden estricto:** I0 → I1 → I2 → I3 → I4 → I5 → I6 (no saltar).

---

## 10. Inventario exacto de archivos

### 10.1 Crear (nuevos)

```
frontend/src/app/estudiante/simulacion-play/
├── data/investigation/
│   ├── clues.catalog.ts
│   ├── contradictions.catalog.ts
│   ├── fact-status.rules.ts
│   ├── hypothesis.options.ts
│   ├── hospital.investigation.ts
│   ├── comisaria.investigation.ts
│   └── case-synthesis.templates.ts
├── services/
│   ├── case-investigation.service.ts
│   ├── conversation.service.ts
│   ├── contradiction.service.ts
│   ├── hypothesis.service.ts
│   └── case-closure.service.ts
├── presentation/
│   ├── investigation-board.component.ts
│   ├── hypothesis-panel.component.ts
│   ├── case-synthesis-panel.component.ts
│   └── finalize-participation-panel.component.ts
└── data/academic-feedback.copy.ts    # Etiquetas UI post-finalizar (Buena decisión, etc.)

frontend/src/app/estudiante/simulacion-play/services/
└── case-investigation.service.spec.ts   # Opcional: reglas contradicción
```

### 10.2 Modificar (existentes)

| Archivo | Cambio principal |
|---------|------------------|
| `simulacion-play.component.ts` | Fases nuevas, integración servicios, `finalizar()`, navegación a resultado |
| `simulacion-play.component.html` | Tablero, hipótesis, síntesis; quitar bitácora plana y barras narrativas %; consecuencias sin `narrativeFeedback` |
| `simulacion-play.component.scss` | Estilos tablero, overlays diegéticos |
| `services/investigation.service.ts` | Delegación a `CaseInvestigationService` o eliminación progresiva |
| `services/narrative-state.service.ts` | Dejar de exponer barras en HUD (o eliminar uso en template) |
| `data/hospital-scene.content.ts` | NPCs/objetos referencian tiers vía `hospital.investigation.ts` |
| `data/comisaria-scene.content.ts` | Idem comisaría |
| `data/scene-types.ts` | Tipos tier, indicadores `!`, umbrales exportados |
| `data/decisions.content.ts` | Quitar `narrativeFeedback` de UI; consecuencias solo narrativas |
| `data/minigames.content.ts` | Textos diegéticos |
| `minigames/timeline-minigame.component.ts` | Sin feedback evaluativo |
| `minigames/route-minigame.component.ts` | Idem |
| `minigames/risk-minigame.component.ts` | Idem |
| `presentation/inworld-dialogue.component.ts` | Solo si hace falta indicador tier (ajuste menor) |
| `resultado.ts` | Secciones 1–3 según §6; mapeo hospital/comisaría |
| `engine/play-scene.engine.ts` | Indicador visual `!` en NPC (opcional I3) |

### 10.3 No tocar

```
backend/**
frontend/src/app/core/services/simulacion.service.ts    # Solo consumir
frontend/src/app/core/services/*.ts (auth, guards)
frontend/src/app/docente/**
frontend/src/app/admin/**
frontend/src/app/estudiante/estudiante.ts
frontend/src/app/estudiante/simulacion.html            # UI quiz legado
frontend/src/app/estudiante/simulacion-play/services/decision-bridge.service.ts
frontend/src/app/estudiante/simulacion-play/data/decision-mapping.config.ts
docker-compose.yml, APIs, modelos Django
```

### 10.4 Referencia / documentación (actualizar al cerrar cada oleada)

```
docs/PLAN_IMPLEMENTACION_INMERSION_INVESTIGATIVA_v3.md   # Este archivo
docs/PLAN_REDISEÑO_INMERSION_MAXIMA.md                  # Anexo histórico v2
docs/INFORME_TECNICO_I*.md                             # Uno por oleada
```

---

## 11. Cambios respecto al código actual (gap analysis)

| Elemento actual | Acción v3 |
|-----------------|-----------|
| Bitácora cronológica lateral | Reemplazar por **Caso en construcción** |
| Barras “Estado narrativo” % | Ocultar o eliminar del HUD |
| `gate-hospital` / `gate-comisaria` con ✓ | Sustituir por señal in-world + umbral |
| `narrativeFeedback` en consecuencia | Eliminar del template |
| Minijuegos con `scoreRatio` y `.feedback.ok` | Solo narrativa neutra |
| `case-continues` sin `finalizar()` | Reemplazar por `case-synthesis` → `finalizar()` → `resultado` |
| `registerNpc` una sola vez | `ConversationService` multi-tier |
| T2 spoilery en `hospital-scene.content.ts` | Reescritura I0 |
| Timeline entre hospital y comisaría | Mover después de consecuencia comisaría |
| `resultado.ts` genérico por pregunta | Bloques Hospital / Comisaría + fortalezas |

---

## 12. Criterios de aceptación (definición de éxito v3)

1. Durante ≥ 15 min de juego, **ninguna** UI muestra correcto/incorrecto ni recomendación de respuesta.  
2. Al menos **2 contradicciones** detectables en hospital y **2** en comisaría.  
3. Tablero **Caso en construcción** con las 6 secciones actualizándose en vivo.  
4. Hipótesis obligatoria antes de cada decisión, **sin nota**.  
5. Decisiones como **acciones profesionales**; `responder()` exactamente **2 veces**.  
6. Tras síntesis, `finalizar()` navega a resultado con retroalimentación **✔ / ⚠** por decisión.  
7. Fortalezas y áreas de mejora visibles **solo** en resultado.  
8. Build Docker sin regresión: `npm run build -- --configuration docker`.

---

## 13. Riesgos y mitigaciones

| Riesgo | Mitigación |
|--------|------------|
| `registerNpc` bloquea T2 | I3 reemplaza registro por `ConversationService.advanceTier()` |
| Mapeo `responder()` falla | Mantener auditoría `DecisionBridgeService`; no avanzar consecuencia si error (ya existe) |
| `detalle_preguntas` no alinea escenarios | Reutilizar keywords de `decision-mapping.config.ts` en resultado |
| Snapshot perdido por `localStorage.clear()` | Escribir en `sessionStorage`; leer antes de limpiar en salir |
| Contenido aún spoilery | Revisión I0 con checklist “¿T1 revela el problema?” |

---

## 14. Autorización para codificar

| Comando del usuario | Acción |
|---------------------|--------|
| `Inicia I0` | Solo catálogos + textos T1 |
| `Inicia I1` | Tablero + servicio caso |
| … | … |
| `Inicia I6` | Cierre + resultado + build |

**Hasta entonces: no commits ni implementación.**

---

*Plan v3 — Alineado con rediseño obligatorio inmersivo + retroalimentación académica únicamente post-`finalizar()`.*
