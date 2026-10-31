# Informe — Implementación Expediente investigativo v4.1

**Fecha:** 2026-06-03  
**Build:** `npm run build -- --configuration=docker` ✓

## Entregado (I0–I6 estudiante)

| Oleada | Estado |
|--------|--------|
| I0 | Catálogos en `data/investigation/*` + diálogos T1–T3 fragmentados |
| I1 | `ExpedienteCasoComponent` — TAB + botón lateral, 7 secciones |
| I2 | Contradicciones automáticas (`contradictions.catalog.ts`) |
| I3 | `ConversationService` — re-entrevista, tiers, sin repetir bloque |
| I4 | Hipótesis + decisión hospital + `responder()` sin evaluación en juego |
| I5 | Comisaría ampliada (5 NPC + 4 objetos), contenido no repetido |
| I6 | Minijuegos al final + síntesis + `finalizar()` + `resultado.ts` 4 bloques |

## Flujo

Hospital (explorar) → Hipótesis → Decisión → Consecuencia → Comisaría → Hipótesis → Decisión → Consecuencia → Minijuegos → Síntesis → `finalizar()` → Resultado.

## Sin evaluación durante el juego

- Eliminadas bitácora, barras narrativas %, `narrativeFeedback` en consecuencias, scoring visible en minijuegos.
- Retroalimentación ✔/⚠ solo en `/estudiante/resultado/:id`.

## No implementado (por spec)

- **D0** analítica docente (no solicitado en comando de implementación).
- **D1** monitor tiempo real (bloqueado).

## Archivos principales nuevos

- `simulacion-play/data/investigation/*`
- `simulacion-play/services/expediente-caso.service.ts`
- `simulacion-play/services/conversation.service.ts`
- `simulacion-play/presentation/expediente-caso.component.ts`
- `estudiante/services/case-result-analysis.ts`
