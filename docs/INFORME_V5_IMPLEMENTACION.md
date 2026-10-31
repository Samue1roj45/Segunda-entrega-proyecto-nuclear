# Informe V5 — Implementación progresiva

**Fecha:** 2026-06-03  
**Build:** `npm run build -- --configuration=docker` ✓

---

## 1. Diagnóstico pantalla negra (Fase 7)

Ver documento completo: [`INFORME_DIAGNOSTICO_CANVAS_NEGRO.md`](./INFORME_DIAGNOSTICO_CANVAS_NEGRO.md)

**Resumen:** Los SVG existen en `public/assets/simulacion/`. El canvas quedaba sin render porque el motor no arrancaba (`ViewChild` estático + canvas en `@if`, y `prepareAssets()` sin `start()` si fallaba una URL).

---

## 2. Archivos frontend modificados / creados

### Nuevos
| Archivo |
|---------|
| `docs/INFORME_DIAGNOSTICO_CANVAS_NEGRO.md` |
| `docs/INFORME_V5_IMPLEMENTACION.md` |
| `data/intro-narrativa.content.ts` |
| `presentation/intro-narrativa.component.ts` |
| `presentation/contenido-sensible.component.ts` |
| `public/assets/simulacion/intro/intro-01-barrio.svg` … `intro-05-hospital.svg` |

### Modificados
| Archivo | Cambio |
|---------|--------|
| `engine/asset-loader.ts` | Carga tolerante a fallos |
| `engine/play-scene.engine.ts` | Resize mínimo, `start()` aunque falle asset |
| `simulacion-play.component.ts` | Flujo intro → advertencia → API; fix canvas |
| `simulacion-play.component.html` | Intro + modal sensible |
| `simulacion-play.component.scss` | Layout canvas a altura completa |

### Sin cambios (ya v4.1)
Expediente 7 secciones, ConversationService T1–T3, contradicciones, hipótesis, `finalizar()`, resultado 4 bloques.

---

## 3. Arquitectura introducción narrativa

```
/estudiante/simulacion
  → IntroNarrativaComponent (5 escenas, imágenes SVG, texto, Siguiente)
  → ContenidoSensibleComponent (modal obligatorio)
  → iniciarParticipacion()
  → explore (canvas + expediente + investigación v4.1)
```

**No** usa `INTRO_STEPS` antiguo ni pantallas "Paso 1 de 3".

---

## 4. Estado por fases V5

| Fase spec | Estado |
|-----------|--------|
| 1 Intro cinematográfica | ✓ Implementada |
| 2 Advertencia contenido | ✓ Implementada |
| 3 Exploración | ✓ (v4.1 + fix canvas) |
| 4 Expediente núcleo | ✓ (v4.1) |
| 5 Contradicciones | ✓ (v4.1) |
| 6 Investigación obligatoria | ✓ Umbrales % antes hipótesis |
| 7 Diagnóstico canvas | ✓ Informe + fix |
| D0/D1 docente | No solicitado en V5 |

---

## 5. Flujo de prueba

1. Dashboard → **Iniciar simulación**
2. Avanzar 5 escenas intro → **Continuar**
3. Modal contenido sensible → **Continuar** (o Salir → dashboard)
4. Canvas hospital con fondo y NPCs; **Tab** abre expediente
5. Investigar hasta 100% → hipótesis → decisión → comisaría → cierre

---

## 6. MemoriasDelParque

No se modificó ni copió código. Solo referencia de arquitectura (exploración, interacción).
