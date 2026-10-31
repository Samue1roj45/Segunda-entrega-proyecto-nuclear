# Plan definitivo — Experiencia investigativa inmersiva

**Estado:** PLANIFICACIÓN APROBADA — **NO IMPLEMENTAR** hasta `Inicia I0`  
**Versión:** 4.1 (alineada con rediseño definitivo 2026-06-03)  
**Restricciones:** Sin backend · Sin APIs nuevas · Sin romper `SimulacionService`, `responder()`, `finalizar()`, `DecisionBridgeService`, `Resultado` API

---

## 0. Principio rector

> El estudiante **no recibe** la historia. **La descubre.**

- Nadie conoce el caso completo.
- Cada fuente aporta un fragmento; algunas se contradicen; algunas ocultan.
- La investigación es el **centro absoluto**; las decisiones cierran cada escenario, no sustituyen la investigación.

**No es el objetivo:** más pantallas, formularios u overlays.  
**Es el objetivo:** sensación de **resolver un caso** (videojuego) + **debriefing profesional** solo al final (evaluación).

---

## 1. Regla de oro

### Durante el juego (hasta `finalizar()`)

| Prohibido |
|-----------|
| Decisión correcta/incorrecta |
| Hipótesis correcta/incorrecta |
| Porcentajes de desempeño |
| Barras de evaluación |
| “Bien” / “Mal” |
| Respuesta correcta / opción recomendada |
| `narrativeFeedback` evaluativo en consecuencias |
| Minijuegos con scoring visible (verde/amarillo, “orden correcto”) |

### Solo al final (`finalizar()` → `/estudiante/resultado/:id`)

| Permitido |
|-----------|
| Retroalimentación académica completa |
| ✔ Buena decisión / ⚠ Oportunidad de mejora (siempre con explicación) |
| Opción recomendada cuando aplique |
| Fortalezas y áreas de mejora |
| Nota / stats del backend (hero existente) |

---

## 2. Centro de la experiencia — Expediente del caso

Sustituye la **bitácora simple**.

### Apertura

- Tecla **TAB** (toggle)
- Botón lateral **permanente**
- Animación tipo **carpeta de investigación**
- El expediente es el **cerebro** de la investigación, no menú secundario

### Componente técnico

`ExpedienteCasoComponent` + `ExpedienteCasoService`

---

## 3. Seis secciones del expediente

### 3.1 Personas

| Inicio | Tras descubrimiento |
|--------|-------------------|
| `???` bloqueado | Avatar, nombre, rol |

**Estados:** Conocido → Entrevistado T1 → T2 → T3

Personas fijas por escenario (hospital + comisaría); desbloqueo al primer contacto.

---

### 3.2 Evidencias

Registro automático al inspeccionar documento/objeto.

- ✓ + **descripción breve**
- Ejemplos: Historia clínica, EPICEE, Expediente comisaría, Ruta de protección

---

### 3.3 Pistas

Registro automático al cerrar diálogo o evidencia.

- **No** clasificar verdadero/falso
- Texto en voz de fuente: *“La madre afirma que…”*, *“El hermano afirma que…”*

---

### 3.4 Contradicciones

Motor automático al coexistir dos pistas de `contradictions.catalog.ts`.

```
⚠ Contradicción detectada
La versión de la madre no coincide con la del hermano.
```

- **No** resolver
- El estudiante debe seguir investigando (T2/T3, otras fuentes)

---

### 3.5 Hechos confirmados (activo del estudiante)

El sistema **propone** hechos candidatos (desbloqueados por pistas).

```
☐ Existió violencia previa
☑ Había una menor presente   ← marcado por el investigador
```

- Al marcar: *“Hecho marcado por el investigador”*
- **No** decir si acertó en juego
- Validación pedagógica **solo** en pantalla final (reglas locales + evidencia del caso)

---

### 3.6 Hipótesis

**Obligatoria** antes de cada decisión (hospital / comisaría).

Opciones ejemplo: información insuficiente · riesgo moderado · riesgo alto · violencia recurrente · situación crítica.

- Sin nota · sin evaluación inmediata · solo almacenar

---

## 4. Diálogos T1 → T3

| Tier | Contenido | Desbloqueo |
|------|-----------|------------|
| **T1** | Superficial, vago | Siempre |
| **T2** | Sensible | Ya investigó algo relacionado |
| **T3** | Profundo | Pistas clave + a menudo contradicción vista |

- **Re-entrevista:** conversación evoluciona; nunca repetir el mismo bloque
- `ConversationService` reemplaza `registerNpc()` idempotente
- Indicadores en mundo: `...` / `!`

**Corrección de contenido actual:** T2/T3 no deben revelar el caso entero ni demasiado pronto.

---

## 5. Investigación por escenario

| Escenario | Rol |
|-----------|-----|
| **Hospital** | Primera investigación completa (no “pantalla previa”) |
| **Comisaría** | Segunda investigación con **información nueva** (no repetir pistas hospital) |

Umbral por profundidad (sustituye 2 NPC + 2 objetos):

**Hospital → hipótesis:** ≥7 pistas · ≥4 personas · ≥2 evidencias · ≥1 contradicción · ≥1 hecho marcado  

**Comisaría → hipótesis:** ≥5 pistas nuevas · ≥2 evidencias CF · ≥2 contradicciones totales · ≥2 personas CF

HUD: *“Investigación 62%”* (checklist), no contador de clicks.

---

## 6. Decisiones

- Lenguaje de **acción profesional** (nunca Opción A/B/C)
- Confirmar → `responder()` en segundo plano
- Overlay consecuencia **solo narrativa** (equipo, familia, protocolo)
- Eliminar `narrativeFeedback` del UI de juego

---

## 7. Minijuegos

**Posición:** después de hospital **y** comisaría (corregir orden actual del código).

Marco diegético: reconstrucción del caso, línea temporal, clasificación de riesgos, organización de evidencias.

- Sin “validar aciertos” visible
- Botón tipo *“Entregar al equipo”* → mensaje narrativo neutro → continuar

---

## 8. Flujo de fases

```
explore-hospital → hypothesis-hospital → decision → consequence
  → explore-comisaria → hypothesis-comisaria → decision → consequence
  → minigames → case-synthesis → finalizar() → resultado
```

Eliminar: `case-continues`, gates con ✓, barras narrativas % en HUD.

---

## 9. Pantalla final (`resultado.ts`)

Datos: `obtenerResultado()` + `CaseClosureSnapshot` (sessionStorage al síntesis).

### Sección 1 — Resumen de investigación

- Personas identificadas
- Evidencias encontradas
- Contradicciones detectadas
- Hechos marcados
- Hipótesis formuladas

### Sección 2 — Retroalimentación académica

Por cada decisión (hospital / comisaría), desde `detalle_preguntas`:

**Si `es_correcta`:** ✔ Buena decisión + por qué + principios + riesgos evitados  

**Si no:** ⚠ Oportunidad de mejora + riesgos + ✔ Opción recomendada + por qué era mejor  

Nunca solo “correcto/incorrecto” sin explicación (`retroalimentacion`, `justificacion` del backend).

### Sección 3 — Fortalezas detectadas

Generadas en frontend (reglas sobre snapshot + resultado).

### Sección 4 — Áreas de mejora

Ej.: pocos hechos confirmados, una sola fuente, poca exploración documental, contradicciones ignoradas (si snapshot lo registra).

**Nota:** Mantener hero con nota del backend; no duplicar evaluación durante el juego.

---

## 10. Docente (dos niveles)

### D0 — Sin backend (autorizable tras I6 o en paralelo tardío)

Nueva vista docente; fuente: `GET /resultados/` + `detalle_preguntas`.

- Decisión más elegida / con más errores
- Pregunta más fallada / más acertada
- Distribución de respuestas

**No incluye** (sin telemetría persistida): contradicción más ignorada, hechos menos marcados en vivo.

### D1 — Requiere backend + autorización explícita `Autoriza D1 backend`

Monitor tiempo real: escenario, NPCs, pistas, contradicciones, hipótesis, progreso.

**No implementar D1** sin esa autorización.

---

## 11. Oleadas (orden estricto)

| Oleada | Entregable |
|--------|------------|
| **I0** | Catálogos + diálogos T1 sin spoiler + pistas fragmentadas |
| **I1** | Expediente (TAB, 6 secciones, UX carpeta) |
| **I2** | Contradicciones + hechos candidatos interactivos |
| **I3** | T1–T3 + re-dialogo + badges canvas |
| **I4** | Hipótesis + umbral + decisión hospital |
| **I5** | Comisaría investigación nueva + hipótesis + decisión |
| **I6** | Minijuegos + síntesis + `finalizar()` + `resultado.ts` 4 secciones |
| **D0** | Analítica grupal docente (opcional) |
| **D1** | Monitor vivo (bloqueado) |

---

## 12. Archivos (resumen)

**Crear:** `data/investigation/*`, `expediente-caso.service.ts`, `conversation.service.ts`, `contradiction.service.ts`, `hypothesis.service.ts`, `case-closure.service.ts`, `expediente-caso.component.ts`, paneles hipótesis/síntesis/finalizar.

**Modificar:** `simulacion-play.component.*`, escenas, `decisions.content.ts`, minijuegos, `resultado.ts`, `narrative-state` (ocultar barras), engine (indicadores).

**No tocar:** `backend/**`, `simulacion.service.ts`, `decision-bridge.service.ts`, auth, guards, admin, `simulacion.html`.

---

## 13. Criterio de éxito

El estudiante termina pensando:

*“Investigué un caso, entrevisté personas, encontré evidencias, detecté contradicciones, formulé hipótesis y tomé decisiones profesionales.”*

**No:**

*“Respondí preguntas en un cuestionario con movimiento.”*

---

## 14. Autorización para codificar

| Comando | Acción |
|---------|--------|
| `Inicia I0` … `Inicia I6` | Implementación estudiante por oleada |
| `Inicia D0` | Analítica docente post-finalización |
| `Autoriza D1 backend` | Monitor tiempo real |

**Hasta entonces: cero commits de implementación.**

---

*Documento único de referencia para el rediseño definitivo. Supersedes detalles conflictivos de v3 salvo donde este archivo sea explícito.*
