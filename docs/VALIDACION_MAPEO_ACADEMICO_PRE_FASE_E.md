# Validación técnica — Mapeo académico (pre Fase E)

**Fecha:** 2026-06-03  
**Alcance:** Sincronización decisiones narrativas → `POST /participaciones/{id}/responder/`  
**Estado:** Corrección aplicada — **Fase E bloqueada hasta aprobación explícita**

---

## 1. Resumen ejecutivo

| Métrica | Antes de corrección | Después de corrección |
|---------|---------------------|------------------------|
| **Opciones narrativas mapeables** (6 total) | 100% solo si el caso usa escenario orden 1/2, pregunta orden 1, respuesta orden 1–3 | 100% en caso alineado; fallback por título de escenario y texto de respuesta |
| **Puntos de decisión** (2: hospital + comisaría) | 100% solo con estructura exacta | ≥1 opción mapeable por escena si existe escenario/pregunta compatible |
| **Trazabilidad al completar simulación** | Riesgo: narrativa avanzaba sin `responder()` | Bloqueo: no hay consecuencia sin registro exitoso en backend |

La simulación registra **2 respuestas académicas** por participación (una por decisión hospital, una por decisión comisaría). No registra las 6 opciones: el estudiante elige **1 de 3** en cada escena.

---

## 2. Porcentaje de mapeo

### Definiciones

- **Opción narrativa:** cada tarjeta de acción (6 en total: 3 hospital + 3 comisaría).
- **Punto de decisión:** momento en que se llama a `responder()` (2 por recorrido completo).

### Cálculo (servicio `DecisionBridgeService.audit()`)

```
optionsMappedPercent = (opciones con ref válido / 6) × 100
decisionsMappedPercent = (escenas con ≥1 opción mapeable / 2) × 100
canCompleteWithFullTraceability = hospital.anyOptionMapped && comisaria.anyOptionMapped
```

### Escenarios típicos

| Estructura del caso en backend | % opciones (antes) | % opciones (después) | Trazabilidad completa |
|-------------------------------|--------------------|----------------------|------------------------|
| Escenario 1 y 2, pregunta 1, 3 respuestas orden 1–3 | **100%** | **100%** | **Sí** |
| Escenarios con títulos “Urgencias” / “Comisaría” pero orden distinto (ej. 3 y 4) | **0%** | **~100%** (por título) | **Sí** |
| Respuestas con orden distinto pero texto compatible | Parcial | **~100%** (por texto) | **Sí** |
| Sin `caso` en participación o sin escenarios | **0%** | **0%** | **No** |
| Un solo escenario en el caso | 50% máx. | 50% máx. | **No** (falta comisaría) |
| Caso con 4+ preguntas no usadas en canvas | 100% en las 2 preguntas mapeadas | 100% en las 2 preguntas mapeadas | **Parcial**¹ |

¹ El estudiante completa la simulación con **2 preguntas respondidas**. Si el caso académico exige más preguntas en otros escenarios, esas quedarán sin responder hasta ampliar la simulación o el docente ajuste el caso.

### Verificación automatizada

Tests en `decision-bridge.service.spec.ts` (Vitest, 5 pruebas OK):

- Caso alineado → 100% / trazabilidad completa.
- Escenario hospital con orden 3 pero título “Urgencias” → hospital 100%.
- Respuesta con `orden` incorrecto → resolución por keywords.
- Sin escenarios → 0% / sin trazabilidad.

Ejecutar:

```bash
cd frontend
npx vitest run src/app/estudiante/simulacion-play/services/decision-bridge.service.spec.ts
```

---

## 3. Qué ocurre cuando orden escenario / pregunta / respuesta no coincide

### Comportamiento anterior (Fase D inicial)

1. `resolve()` buscaba solo `escenario.orden === config`, `pregunta.orden === config`, `respuesta.orden === config`.
2. Si fallaba → `null`, **pero la UI igual pasaba a consecuencia** y solo mostraba snackbar opcional.
3. El estudiante podía terminar el flujo con **0–1–2** registros en backend impredecibles.

### Comportamiento actual (corregido)

Resolución en cascada (`DecisionBridgeService`):

| Paso | Estrategia | Condición |
|------|------------|-----------|
| 1 | Orden exacto escenario | `escenario.orden === escenarioOrden` (1 hospital, 2 comisaría) |
| 2 | Escenario por título/narrativa | Keywords en `decision-mapping.config.ts` |
| 3 | Pregunta | `pregunta.orden === preguntaOrden`; si no, única pregunta del escenario; si no, primera por orden |
| 4 | Respuesta por orden | `respuesta.orden === respuestaOrden` |
| 5 | Respuesta por texto | Keywords por `option.id` en `OPTION_BACKEND_KEYWORDS` |
| 6 | Respuesta por índice | Posición `respuestaOrden - 1` en lista ordenada |

Si tras la cascada no hay ref:

- **No** se aplica consecuencia narrativa.
- **No** se llama a `responder()`.
- Mensaje en pantalla de confirmación + snackbar.

Si `responder()` falla (red, validación API):

- **No** se avanza a consecuencia.
- El estudiante puede reintentar confirmar.

Al iniciar participación:

- Se ejecuta `audit(caso)` y se registra en consola (`console.warn`) si `canCompleteWithFullTraceability === false`.

---

## 4. Decisiones narrativas que quedarían sin registro académico

### Por opción (las 6 tarjetas)

Solo la opción **elegida** por el estudiante se envía a `responder()`. Las otras 5 no se registran (comportamiento esperado de un MCQ por escena).

Opciones **no registrables** (ningún estudiante podría mapearlas) si:

- El caso no trae escenarios/preguntas/respuestas.
- No existe escenario reconocible para hospital o comisaría.
- La pregunta del escenario no tiene respuestas, o ninguna coincide por orden/texto/índice.

En auditoría: `OptionMappingStatus.mapped === false` para esa opción.

### Por recorrido completo

| Situación | ¿Pierde trazabilidad? |
|-----------|------------------------|
| Caso bien configurado (2 escenarios, 1 pregunta c/u, 3 respuestas) | **No** — 2 POST `responder` |
| Caso sin estructura | **Sí** — bloqueado en confirmación |
| API caída al confirmar | **Sí** temporal — reintentable, sin avance |
| Caso con preguntas extra no ligadas al canvas | **Parcial** — esas preguntas quedan sin responder |

---

## 5. Ejemplo real de mapeo

Caso de referencia (mock en tests, representativo de un caso docente bien configurado):

### Hospital — “Activar protocolo de atención integral”

```
Decisión narrativa: "Activar protocolo de atención integral"
→ Escenario backend #101 (orden 1, título: "Urgencias hospitalarias")
→ Pregunta backend id=1001 (orden 1)
→ Respuesta backend id=10001 (orden 1, estrategia: respuesta-por-orden)
```

**POST:** `{ pregunta_id: 1001, respuesta_id: 10001 }`

### Comisaría — “Activar medida de protección inmediata”

```
Decisión narrativa: "Activar medida de protección inmediata"
→ Escenario backend #102 (orden 2, título: "Comisaría de Familia")
→ Pregunta backend id=1002 (orden 1)
→ Respuesta backend id=10004 (orden 1, estrategia: respuesta-por-orden)
```

**POST:** `{ pregunta_id: 1002, respuesta_id: 10004 }`

### Tabla config narrativo → backend

| Opción narrativa (`id`) | `escenarioOrden` | `preguntaOrden` | `respuestaOrden` |
|-------------------------|------------------|-----------------|------------------|
| protocolo-integral | 1 | 1 | 1 |
| entrevista-familiar | 1 | 1 | 2 |
| esperar-valoracion | 1 | 1 | 3 |
| medida-inmediata | 2 | 1 | 1 |
| acompanamiento-gradual | 2 | 1 | 2 |
| conciliacion | 2 | 1 | 3 |

---

## 6. ¿Puede completarse la participación sin perder trazabilidad?

### En la simulación (Fase D)

| Requisito | Estado |
|-----------|--------|
| Exploración + 2 decisiones registradas en backend | **Sí**, si `audit().canCompleteWithFullTraceability` |
| Minijuegos | No llaman `responder()` (solo estado narrativo local) |
| Pantalla “Caso en curso” | Participación sigue **EN_CURSO** (sin `finalizar()`) |

### Condición para Fase E

Antes de implementar `finalizar()`, el docente debe verificar en consola del navegador (o ampliar UI docente) que al cargar `/estudiante/simulacion`:

```javascript
// Tras iniciar participación — audit interno
canCompleteWithFullTraceability === true
optionsMappedPercent === 100  // recomendado
```

Si el caso real de la práctica no alcanza 100%, ajustar en admin/docente:

- Escenario 1: título con “urgencias” / “hospital” / “EPICEE”.
- Escenario 2: título con “comisaría” / “familia”.
- Una pregunta por escenario (orden 1).
- Tres respuestas por pregunta (orden 1–3) con textos reconocibles por las keywords de `decision-mapping.config.ts`.

---

## 7. Archivos de la corrección

| Archivo | Cambio |
|---------|--------|
| `decision-bridge.service.ts` | Cascada de mapeo + `audit()` |
| `decision-mapping.config.ts` | Keywords escena/opción |
| `decision-bridge.service.spec.ts` | Tests de porcentaje y fallbacks |
| `simulacion-play.component.ts` | Bloqueo sin `responder` exitoso |
| `simulacion-play.component.html` | Avisos de mapeo en confirmación |

---

## 8. Conclusión para Fase E

| Pregunta | Respuesta |
|----------|-----------|
| ¿% mapeo con caso alineado? | **100%** (6/6 opciones, 2/2 decisiones) |
| ¿% con caso mal configurado? | **0%** — flujo bloqueado en confirmación |
| ¿Se pierde trazabilidad al terminar simulación? | **No**, si el caso pasa auditoría y API responde OK |
| ¿Listo para `finalizar()`? | **Solo tras validar el caso real de producción** con `audit()` |

**Fase E no iniciada.** Esperar **APROBADO Fase E** explícito tras validar el caso cargado en tu entorno (Docker/práctica activa).
