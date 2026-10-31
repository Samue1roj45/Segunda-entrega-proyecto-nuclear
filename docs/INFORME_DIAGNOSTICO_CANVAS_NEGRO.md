# Informe técnico — Pantalla negra en canvas (Fase 7 V5)

**Fecha:** 2026-06-03  
**Ámbito:** `frontend/src/app/estudiante/simulacion-play`

---

## Síntoma

El **HUD** (escena, temporizador, contador de pistas) se muestra, pero el **canvas del escenario** aparece vacío o negro: sin fondo hospital/comisaría, sin NPCs ni jugador.

---

## Verificación de assets

| Asset | Ruta esperada | Estado en repo |
|-------|---------------|----------------|
| `hospital-bg.svg` | `/assets/simulacion/hospital-bg.svg` | ✓ `frontend/public/assets/simulacion/` |
| `comisaria-bg.svg` | `/assets/simulacion/comisaria-bg.svg` | ✓ |
| `player.svg` | `/assets/simulacion/player.svg` | ✓ |
| `object-doc.svg` | `/assets/simulacion/object-doc.svg` | ✓ |
| `npc-generic.svg` | `/assets/simulacion/npc-generic.svg` | ✓ (no usado por `AssetLoader` actual) |

`angular.json` copia `public/**` al build. En Docker el bundle sirve esos archivos en la raíz del host.

**Conclusión:** Los archivos existen; el fallo no es ausencia en disco sino **tiempo de vida del canvas / arranque del motor**.

---

## Causas raíz identificadas

### 1. `ViewChild` estático + canvas dentro de `*@if`

```html
@if (!loading() && !errorInicio() && showHud()) {
  ...
  @if (showCanvas()) {
    <canvas #gameCanvas>
```

- `@ViewChild('gameCanvas', { static: true })` se resuelve **antes** de que el canvas exista (durante `loading === true` el nodo no está en el DOM).
- `ngAfterViewInit` dispara `initEngineWhenReady` cuando aún puede no haber `<canvas>`.
- El polling al terminar `loading` llama `mountEngine()`, pero la referencia puede quedar **inválida o sin elemento**.

**Impacto:** `PlaySceneEngine` no arranca o `start()` nunca se ejecuta → sin bucle `requestAnimationFrame` → canvas sin dibujo (negro/transparente).

### 2. `prepareAssets()` sin manejo de error

```typescript
void this.engine.prepareAssets().then(() => {
  this.engine?.start();
});
```

Si **una** URL falla (404, red, CORS), la promesa rechaza y **`start()` no se llama**. El motor queda creado pero sin loop de render.

`AssetLoader.loadAll()` usa `Promise.all` — un fallo tumba toda la carga.

### 3. Dimensiones del canvas en `resize()`

`resize()` usa `parent.clientWidth/Height`. Si el padre tiene altura 0 en el primer frame (flex sin altura resuelta), el buffer del canvas queda en **0×0** → nada visible aunque el loop corra.

Hay `min-height: 480px` en `.canvas-wrap`, pero reglas **duplicadas** de `.main-row` (flex vs grid) pueden impedir que el flex hijo reciba altura.

### 4. Qué NO es la causa principal

- El renderer tiene **fallback** de color si falla el SVG (`#7a9bb0` hospital).
- Con motor activo nunca debería verse negro puro salvo buffer 0×0 o loop detenido.
- `npc-generic.svg` no se carga en código actual; los NPC se dibujan con primitivas (`drawCharacter`), no dependen de ese sprite.

---

## Correcciones aplicadas (V5)

1. `ViewChild` → `{ static: false }` + `scheduleMountEngine()` tras vista estable (`setTimeout` + `AfterViewChecked` guard opcional).
2. `AssetLoader`: carga tolerante a fallos (registra error, continúa con fallbacks).
3. `prepareAssets().catch()` → iniciar loop igual con `assetsReady` parcial.
4. `resize()` forzado tras `start()` y en `ResizeObserver`; altura mínima explícita en `.main-row` / `.canvas-wrap`.
5. Flujo V5: intro + advertencia **antes** de `iniciarParticipacion()` y montaje del canvas (evita doble condición `loading`).

---

## Cómo validar

1. Abrir `/estudiante/simulacion` → intro → advertencia → Continuar.
2. DevTools → Network: `hospital-bg.svg` y `player.svg` → **200**.
3. Debe verse fondo ilustrado, personajes con etiquetas y jugador cyan.
4. Consola: sin `No se pudo cargar: /assets/simulacion/...`.

---

## Archivos tocados por el fix

- `engine/asset-loader.ts`
- `engine/play-scene.engine.ts`
- `simulacion-play.component.ts`
- `simulacion-play.component.scss`
- `presentation/intro-narrativa.component.ts` (nuevo flujo previo al canvas)
