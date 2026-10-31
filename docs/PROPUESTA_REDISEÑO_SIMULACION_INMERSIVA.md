# Propuesta de rediseño — Simulación inmersiva

**Estado del proyecto:** DESARROLLO DETENIDO  
**Fase E:** NO APROBADA — NO INICIAR  
**Fecha:** 2026-06-03  
**Motivo:** La implementación actual no cumple la visión de simulación inmersiva (“explorar → descubrir → interpretar → decidir”).

---

## 1. Resumen ejecutivo

Hoy la ruta `/estudiante/simulacion` funciona como **presentación textual + canvas abstracto + overlays de decisión**. El estudiante **lee el caso antes de jugar** y, al entrar al “juego”, ve formas geométricas sobre fondos oscuros — no una situación profesional reconocible.

Esta propuesta define **cómo rediseñar la experiencia** sin copiar otro proyecto (p. ej. MemoriasDelParque), manteniendo Angular + canvas liviano y la integración académica ya validada (`responder()`, bitácora, gating).

**No se implementará nada** hasta aprobación explícita de esta propuesta (o una versión ajustada por el equipo).

---

## 2. Diagnóstico — Problemas detectados vs. implementación actual

| # | Problema reportado | Evidencia en código actual | Impacto en experiencia |
|---|-------------------|---------------------------|------------------------|
| 1 | Historia por pantallas de texto | `intro.content.ts` — 3 pasos; `phase === 'intro'` bloquea canvas | Sensación de “presentación PowerPoint” antes del juego |
| 2 | El caso no se descubre jugando | Contexto del barrio, agresión y rol profesional están **solo** en intro | El estudiante ya “sabe” el caso antes de investigar |
| 3 | Canvas sin experiencia visual clara | `play-scene.engine.ts`: gradientes + rectángulos + círculos; **sin sprites ni fondos** | Pantalla oscura / “vacía”; no se identifica hospital ni personajes |
| 4 | No parece videojuego educativo | Flujo dominado por overlays HTML (intro, gates, decisiones, minijuegos en formularios) | “Leer → responder”, no “vivir → averiguar” |
| 5 | Falta revisión de render y descubrimiento | Jugador: cápsula cyan 28×40px; NPCs: círculo + etiqueta; objetos: emoji en círculo | Interacción existe en lógica, no en **percepción** |
| 6 | Caso no se revela progresivamente | Pistas en diálogos modales HTML separados del mundo | Información correcta en datos, **mala entrega inmersiva** |

### Lo que sí funciona (conservar en rediseño)

- API `iniciarParticipacion()` / `responder()` con mapeo robusto (`DecisionBridgeService`).
- Bitácora, gating (2 NPC + 2 objetos), estado narrativo local.
- Estructura de contenido por escena (`hospital-scene.content.ts`, NPCs, objetos, clues).
- Motor de movimiento, colisiones, proximidad, tecla E / clic.

El fallo no es la **arquitectura de datos**, sino la **capa de experiencia** (presentación + render + ritmo narrativo).

---

## 3. Auditoría técnica del estado actual

### 3.1 Render del escenario

**Archivo:** `engine/play-scene.engine.ts`

| Elemento | Implementación actual | Percepción del jugador |
|----------|----------------------|------------------------|
| Hospital | Gradiente azul oscuro + rectángulo gris 880×420 + texto “URGENCIAS” | Sala genérica, sin camillas, puertas, monitores, señalética |
| Comisaría | Gradiente índigo + rectángulo + texto “COMISARÍA” | Oficina abstracta |
| Obstáculos | Rectángulos `#1e293b` | Bloques sin significado |
| Escala | `scale = min(w/W, h/H)` con letterboxing | Bandas vacías laterales/superiores si el contenedor es alto |

**Causa probable de “pantalla negra”:** fondo `#0f172a` / `#1e1b4b` + área jugable centrada pequeña + sin iluminación focal ni props reconocibles.

### 3.2 Render del jugador

**Archivo:** `engine/play-player.ts` + `drawPlayer()`

- Sprite procedural: elipse + rectángulo + círculo (cabeza).
- Sin animación de caminar (solo desplazamiento).
- Sin sprite sheet ni dirección visual diferenciada más allá de `direction` (no usado en dibujo).

### 3.3 Render de NPCs

- Círculo de color (`npc.color`) + cabeza blanca + `shortLabel` debajo.
- Sin postura, rol visual, ni estado (p. ej. “alterada”, “ocupado”).
- Sin indicador de “hay diálogo disponible” hasta estar muy cerca.

### 3.4 Render de objetos

- Círculo amarillo semitransparente + emoji (`obj.icon`) + label.
- No parecen “historia clínica” o “expediente” en el mundo — parecen íconos flotantes.

### 3.5 Indicadores de interacción

- Línea punteada cyan + caja `[E] o clic — {label}` cuando `nearest` está en rango.
- **Adecuado en lógica**, insuficiente en affordance visual (falta icono sobre NPC, pulso, brillo).

### 3.6 Flujo de descubrimiento del caso

```
[iniciar API] → intro (3 pantallas texto) → explore hospital → diálogos modal HTML
→ gate → decisión overlay → minijuego HTML → comisaría → …
```

| Fuente de información | ¿Existe? | ¿Inmersivo? |
|----------------------|----------|-------------|
| Intro expositiva | Sí | **No** — contradice “descubrir jugando” |
| Diálogos NPC | Sí (contenido rico) | Parcial — modal fuera del mundo |
| Documentos (objetos) | Sí | Parcial — mismo patrón modal |
| Observación del entorno | **No** | Sin hotspots ambientales ni inspectables en escena |
| Hallazgos bitácora | Sí (panel lateral) | Útil, pero secundario si el mundo no engancha |

**Conclusión:** El caso **está escrito** para descubrimiento progresivo en datos, pero el **primer contacto** es expositivo (intro) y el **mundo** no comunica situación sin leer.

---

## 4. Visión objetivo (criterios de aceptación)

Cuando el estudiante entre a la simulación debe:

1. **Aparecer en el hospital** (sin slideshow previo del caso completo).
2. **Ver** una sala reconocible, su personaje, personas y objetos relevantes.
3. **Escuchar / leer en contexto** testimonios al acercarse e interactuar (no ficha externa).
4. **Ir armando el puzzle** en bitácora: cada pieza solo después de interacción.
5. **Interpretar** y luego **decidir** — decisiones como acciones profesionales en el mundo o panel diegético (no cuestionario).

Frase guía:

> **Explorar → descubrir → interpretar → decidir**  
> No: **Leer historia → responder**

---

## 5. Propuesta de rediseño — Pilares

### Pilar A — Eliminar la intro expositiva

| Acción | Detalle |
|--------|---------|
| Eliminar fase `intro` y `INTRO_STEPS` como pantallas completas | Sustituir por **spawn directo** en hospital |
| Tutorial mínimo diegético | 1 sola línea en HUD: “WASD mover · E interactuar” (3 s, desvanecer) |
| Contexto inicial solo ambiental | Sonido/visual: sirena lejana, luces urgencias, panel “Triage” — **sin** párrafos del barrio |

El barrio, la agresión y la menor se revelan **solo** vía madre, hermano, médico, historia clínica, EPICEE.

### Pilar B — Mundo legible (capa visual)

**Opción recomendada (sin motor pesado):** canvas 2D con **tilemap + sprites 2D** (PNG/WebP propios o pack libre CC0).

| Capa | Contenido |
|------|-----------|
| 0 — Fondo | Tilemap hospital (suelo, paredes, puerta, ventana) |
| 1 — Props | Camilla, monitor, carpeta, mesa comisaría |
| 2 — Entidades | Sprites jugador (4 direcciones × 2 frames), NPCs diferenciados |
| 3 — UI mundo | Icono “!” o “…” sobre interactuables; zona de luz en objetos clave |
| 4 — HUD canvas | Flecha hacia objetivo opcional tras primera pista |

**Mínimo viable visual (MVP rediseño):**

- 1 imagen de fondo por escena (1920×1080 recortada, no solo color plano).
- 4 sprites NPC + 1 jugador + 2 iconos objeto — aunque sean ilustraciones simples consistentes.
- Zona de juego = 100% del canvas (reducir letterboxing negro).

**No usar** código ni assets de MemoriasDelParque — solo referencia de **patrones** (escena, entidades, diálogo in-game).

### Pilar C — Diálogo inmersivo

| Actual | Propuesto |
|--------|-----------|
| Modal HTML fullscreen oscuro | **Caja de diálogo sobre canvas** (bottom 25%), retrato NPC, texto typewriter opcional |
| Cerrar = clic fuera | Avanzar con E / clic en “Continuar” |
| Título genérico | Nombre + rol (“Madre de la paciente”) |

Exploración **no se pausa** en modo “solo lectura” opcional; por defecto pausa suave (NPC animado idle).

### Pilar D — Descubrimiento progresivo (diseño de contenido)

**Curva de revelación hospital (ejemplo):**

| Orden sugerido | Interacción | Qué descubre el estudiante |
|----------------|-------------|----------------------------|
| 1 | Acercarse a zona triage / médico | Hay paciente crítica, arma blanca, menor en hogar |
| 2 | Madre | Amenazas previas, control, miedo a denunciar |
| 3 | Hermano | Dinámica familiar, antecedentes |
| 4 | Historia clínica | Lesiones, datos objetivos |
| 5 | EPICEE | Marco de valoración integral |

**Gating** se mantiene (≥2 NPC, ≥2 objetos) pero el estudiante **no sabe el caso completo** al entrar.

**Nuevos tipos de interacción (fase rediseño):**

- **Observables ambientales:** p.ej. “Monitor”, “Puerta familia” — línea corta en bitácora sin diálogo largo.
- **NPCs bloqueados** hasta tener pista X (opcional): médico da más detalle si ya viste historia clínica.

### Pilar E — Decisiones y minijuegos diegéticos

- Decisión hospital: abrirse desde **NPC coordinador** o **puesto de trabajo**, no overlay genérico tipo formulario.
- Minijuegos: marco narrativo (“Reordenar eventos en la pizarra del equipo”) con **misma estética** que el canvas, no solo Material form en centro.

Integración `responder()` **sin cambios conceptuales** — solo el momento de confirmación sigue siendo post-acción narrativa.

### Pilar F — Layout de pantalla

```
┌─────────────────────────────────────────────────────────┐
│ HUD mínimo: escena · reloj · pistas (N)                 │
├──────────────────────────────────┬──────────────────────┤
│                                  │ Bitácora (colapsable) │
│         CANVAS 70–80%            │ descubrimientos      │
│    (mundo + diálogo in-canvas)   │ estado narrativo     │
│                                  │                      │
└──────────────────────────────────┴──────────────────────┘
```

En móvil: canvas full width; bitácora drawer inferior.

---

## 6. Arquitectura propuesta (frontend)

```
simulacion-play/
├── engine/
│   ├── play-scene.engine.ts      → orquestador
│   ├── scene-renderer.ts         → capas tilemap + props + entidades
│   ├── sprite-atlas.ts           → carga imágenes, frames
│   ├── play-player.ts
│   └── discovery-controller.ts   → orden revelación, NPCs bloqueados
├── presentation/
│   ├── inworld-dialogue.component.ts  → UI sobre canvas (no fullscreen)
│   └── discovery-toast.ts             → “Nueva pista” sin romper inmersión
├── assets/simulacion/            → NUEVO: png/webp por escena
│   ├── hospital/
│   └── comisaria/
├── data/                         → contenido (sin intro expositiva)
└── services/                     → investigation, bridge (mantener)
```

**Fases de implementación sugeridas (nueva numeración — NO Fase E):**

| Fase | Nombre | Entregable verificable |
|------|--------|------------------------|
| **R0** | Parada y limpieza UX | Quitar intro 3 pantallas; spawn hospital; HUD tutorial 3 s |
| **R1** | Visual MVP | Fondo + sprites jugador/NPC/objeto; indicadores ! ; sin bandas negras |
| **R2** | Diálogo in-world | Caja sobre canvas; bitácora sigue registrando |
| **R3** | Descubrimiento dirigido | Observables ambientales; curva de pistas; sin spoilers iniciales |
| **R4** | Decisiones diegéticas | Tarjetas desde punto del mundo; mantener `responder()` |
| **R5** | Minijuegos integrados visualmente | Misma línea gráfica |
| **R6** | Cierre académico | `finalizar()` + resultado (antigua Fase E) |

Cada fase requiere **aprobación explícita** y demo jugable antes de la siguiente.

---

## 7. Matriz de revisión solicitada

### Render del escenario

| Criterio | Hoy | Objetivo rediseño |
|----------|-----|-------------------|
| Identificación inmediata del lugar | No | Sí — hospital/comisaría reconocibles en &lt;3 s |
| Profundidad visual | Plana | Fondo + props + sombras ligeras |
| Área útil del canvas | ~60% útil | ≥90% |

### Render del jugador

| Criterio | Hoy | Objetivo |
|----------|-----|----------|
| Visible y distinguible | Parcial | Sprite humano claro |
| Animación al mover | No | 2–4 frames por dirección |
| Posición en escena | Sí | Sí |

### Render NPCs

| Criterio | Hoy | Objetivo |
|----------|-----|----------|
| Diferenciación por rol | Solo color | Sprite + uniforme/vestimenta |
| Indicador interactuable | Solo al estar pegado | Icono ! a distancia media |
| Emoción/contexto | No | Retrato en diálogo |

### Render objetos

| Criterio | Hoy | Objetivo |
|----------|-----|----------|
| Parecen documentos del caso | No (emoji) | Sprite carpeta/tablet + label |
| Destacan sin romper inmersión | Brillo amarillo | Pulso sutil + tooltip corto |

### Flujo de descubrimiento

| Criterio | Hoy | Objetivo |
|----------|-----|----------|
| Caso en intro | Sí — mal | No |
| Caso solo por juego | No | Sí |
| Progresión | Lineal libre | Curva diseñada + bitácora |
| Primera sensación al entrar | “Leer” | “Estoy en urgencias, ¿qué pasó?” |

---

## 8. Riesgos y restricciones

| Riesgo | Mitigación |
|--------|------------|
| Assets sin diseñador | Pack CC0 + paleta única; ilustración simple coherente |
| Peso de imágenes | WebP, atlas por escena, lazy load al cambiar comisaría |
| Scope creep | R1 solo visual + quitar intro; no tocar backend |
| Regresión académica | No tocar `DecisionBridgeService` hasta R4 |
| Confusión con “quiz” | Decisiones siempre verbos de acción profesional |

**Restricciones que se asumen vigentes:**

- Angular + Material en shell; canvas para mundo.
- Sin Unity/Phaser completo.
- Backend/API sin cambios salvo acordar otro canal.

---

## 9. Qué NO hacer hasta nueva aprobación

- ❌ Fase E (`finalizar()`, resultado final).
- ❌ Más pantallas de texto narrativo previas al juego.
- ❌ Minijuegos solo como formularios Material desconectados del mundo.
- ❌ Ampliar contenido académico sin arreglar la capa visual.

---

## 10. Decisión requerida del equipo

Para reanudar desarrollo, indicar una de:

1. **Aprobar propuesta R0→R1** (quitar intro + visual MVP) — recomendado como primer hito.  
2. **Ajustar propuesta** (p. ej. exigir ilustración custom, o permitir Phaser-lite).  
3. **Mantener detenido** hasta definir assets con diseñador.

**Respuesta sugerida:** `APROBADO R0+R1` o lista de cambios a esta propuesta.

---

## 11. Anexo — Referencia de archivos actuales a tocar en rediseño

| Archivo | Cambio previsto |
|---------|-----------------|
| `data/intro.content.ts` | Eliminar o reducir a 0 pasos |
| `simulacion-play.component.ts` | Fase inicial = `explore` hospital |
| `engine/play-scene.engine.ts` | Delegar dibujo a `scene-renderer` + assets |
| `simulacion-play.component.html` | Diálogo in-world; canvas más grande |
| `data/hospital-scene.content.ts` | Añadir observables ambientales (opcional R3) |
| Nuevo `assets/simulacion/**` | Sprites y fondos |

---

*Documento de propuesta únicamente. No implica implementación ni aprobación de Fase E.*
