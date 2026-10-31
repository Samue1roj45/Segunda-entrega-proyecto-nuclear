# Informe de auditoría V7 — Uso real de escenarios SVG en el juego

**Fecha:** 2026-06-04  
**Alcance:** Verificación estática del código y del filesystem (sin implementar cambios).

---

## Resumen ejecutivo

| Hallazgo | Estado |
|----------|--------|
| Los 6 tiles hospital V7 en `public/assets/simulacion/hospital/zones/` | **SÍ se cargan y se dibujan** en fase `explore` |
| Nombre `hospital-espera.svg` solicitado en la auditoría | **NO existe**; el archivo real es `sala-espera.svg` (`tileId: 'sala-espera'`) |
| Prefijos `docs/v7-capturas/hospital-*.svg` | **NO usados** por el juego (copias de documentación) |
| `hospital-bg.svg` / `comisaria-bg.svg` (V5) | **SÍ se usan** como capa tenue bajo las zonas (`drawBackground`, alpha 0.12) |
| Hospital y comisaría comparten el mismo pipeline de render | Difieren solo en `scene.id` y en `HOSPITAL_SCENE` vs `COMISARIA_SCENE` |

---

## Cadena de render (momento exacto)

1. Ruta `/estudiante/simulacion` → `Simulacion` → `SimulacionPlayComponent`.
2. Tras intro y advertencia, `phase === 'explore'` y `showCanvas()` es true.
3. `mountEngine()` crea `PlaySceneEngine` y llama `prepareAssets()` → `AssetLoader.loadAll()`.
4. `engine.start()` registra `requestAnimationFrame(loop)`.
5. En cada frame, `loop` termina llamando `draw()` (aunque el juego esté pausado por diálogo, `draw()` sigue ejecutándose; la lógica de movimiento se salta si `paused`).
6. Dentro de `draw()`, orden de capas:
   - `drawBackground` (color sólido + `hospital-bg.svg` o `comisaria-bg.svg` al 12%)
   - **`drawZones`** → por cada zona: **`drawZoneTile`** → **`ctx.drawImage(tile, …)`** con el SVG de zona
   - `drawFurniture`, `drawObjects`, `drawNpcs`, `drawPlayer`

**Función que “renderiza el hospital”:** no hay una función con ese nombre. El hospital es `PlaySceneEngine` con `this.scene === HOSPITAL_SCENE` (`scene.id === 'hospital'`). El dibujo lo hace `SceneRenderer` invocado desde `PlaySceneEngine.draw()`.

**Función que “renderiza la comisaría”:** la misma (`PlaySceneEngine` + `SceneRenderer`) con `COMISARIA_SCENE` (`scene.id === 'comisaria'`).

---

## Qué fondo se dibuja actualmente

### Capa 1 — Lienzo exterior (viewport)

```229:232:frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts
  private draw(): void {
    const ctx = this.ctx;
    ctx.fillStyle = '#475569';
    ctx.fillRect(0, 0, this.width, this.height);
```

### Capa 2 — Fondo de escena (`drawBackground`)

```15:25:frontend/src/app/estudiante/simulacion-play/engine/scene-renderer.ts
  drawBackground(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    const W = scene.worldWidth;
    const H = scene.worldHeight;
    ctx.fillStyle = scene.id === 'comisaria' ? '#e2e8f0' : '#cbd5e1';
    ctx.fillRect(0, 0, W, H);
    const bg = this.assets.bgFor(scene.id);
    if (bg) {
      ctx.globalAlpha = 0.12;
      ctx.drawImage(bg, 0, 0, W, H);
      ctx.globalAlpha = 1;
    }
  }
```

- **Hospital:** rectángulo `#cbd5e1` + imagen `/assets/simulacion/hospital-bg.svg` (opacidad 12%).
- **Comisaría:** rectángulo `#e2e8f0` + imagen `/assets/simulacion/comisaria-bg.svg` (opacidad 12%).

### Capa 3 — Zonas V7 (tiles SVG, opaco)

Cada entrada en `scene.zones` se pinta con su SVG en `drawZoneTile` (ver abajo). Esta es la capa visual principal V7.

### Capa 4 — Puertas, paredes, entidades

`drawDoors`, `drawWalls` (obstáculos delgados), NPCs, objetos, jugador.

---

## Tabla por SVG hospital (auditoría solicitada)

> **Nota de nomenclatura:** En el repositorio **no hay** archivos llamados `hospital-recepcion.svg`, `hospital-espera.svg`, etc. Los nombres en disco son `recepcion.svg`, `sala-espera.svg`, … dentro de `hospital/zones/`. En `docs/v7-capturas/` existen copias con prefijo `hospital-` que **el código no referencia**.

### 1. Recepción (solicitado: `hospital-recepcion.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/recepcion.svg` |
| **URL en runtime** | `/assets/simulacion/hospital/zones/recepcion.svg` |
| **TS que define la carga** | `frontend/src/app/estudiante/simulacion-play/engine/asset-loader.ts` (`HOSPITAL_ZONES`, `loadAll`) |
| **TS que enlaza zona → tile** | `frontend/src/app/estudiante/simulacion-play/data/hospital-scene.content.ts` (`tileId: 'recepcion'`) |
| **Método que carga** | `AssetLoader.loadAll()` → `loadImage(url)` |
| **Método que dibuja** | `SceneRenderer.drawZoneTile()` → `ctx.drawImage(tile, zone.x, zone.y, zone.w, zone.h)` |
| **Momento del render** | Cada frame de `PlaySceneEngine.loop` → `draw()` → `drawZones()` cuando `assetsReady === true` y escena activa es hospital |

### 2. Sala de espera (solicitado: `hospital-espera.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/sala-espera.svg` |
| **Estado del nombre solicitado** | **`hospital-espera.svg` NO EXISTE** en el proyecto (búsqueda 0 archivos) |
| **URL en runtime** | `/assets/simulacion/hospital/zones/sala-espera.svg` |
| **TS que define la carga** | `asset-loader.ts` — entrada `'sala-espera'` en `HOSPITAL_ZONES` |
| **TS que enlaza zona → tile** | `hospital-scene.content.ts` — `id: 'sala-espera'`, `tileId: 'sala-espera'` |
| **Método que carga** | `loadAll()` → `loadImage()` |
| **Método que dibuja** | `drawZoneTile()` → `drawImage` |
| **Momento del render** | Igual que recepción |

### 3. Pasillo (`hospital-pasillo.svg` → archivo `pasillo.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/pasillo.svg` |
| **URL** | `/assets/simulacion/hospital/zones/pasillo.svg` |
| **Carga** | `HOSPITAL_ZONES` → `loadAll` → `loadImage` |
| **Enlace** | `hospital-scene.content.ts` — `tileId: 'pasillo'` |
| **Dibuja** | `drawZoneTile` |
| **Render** | Cada frame en fase `explore` |

### 4. Enfermería (`hospital-enfermeria.svg` → `enfermeria.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/enfermeria.svg` |
| **URL** | `/assets/simulacion/hospital/zones/enfermeria.svg` |
| **Carga** | `HOSPITAL_ZONES` → `loadAll` → `loadImage` |
| **Enlace** | `hospital-scene.content.ts` — `tileId: 'enfermeria'` |
| **Dibuja** | `drawZoneTile` |
| **Render** | Cada frame en fase `explore` |

### 5. UCI (`hospital-uci.svg` → `uci.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/uci.svg` |
| **URL** | `/assets/simulacion/hospital/zones/uci.svg` |
| **Carga** | `HOSPITAL_ZONES` → `loadAll` → `loadImage` |
| **Enlace** | `hospital-scene.content.ts` — `tileId: 'uci'` |
| **Dibuja** | `drawZoneTile` |
| **Render** | Cada frame en fase `explore` |

### 6. Archivo clínico (`hospital-archivo.svg` → `archivo.svg`)

| Campo | Evidencia |
|-------|-----------|
| **Ruta exacta** | `frontend/public/assets/simulacion/hospital/zones/archivo.svg` |
| **URL** | `/assets/simulacion/hospital/zones/archivo.svg` |
| **Carga** | `HOSPITAL_ZONES` → `loadAll` → `loadImage` |
| **Enlace** | `hospital-scene.content.ts` — `tileId: 'archivo'` |
| **Dibuja** | `drawZoneTile` |
| **Render** | Cada frame en fase `explore` |

---

## Assets existentes pero NO utilizados por el juego

| Ruta | Motivo |
|------|--------|
| `docs/v7-capturas/hospital-*.svg` | Copias para documentación; ningún `.ts` importa ni referencia `docs/v7-capturas` |
| `docs/v7-capturas/comisaria-*.svg` | Idem |
| `hospital-espera.svg` (cualquier ruta) | **Archivo inexistente**; el juego usa `sala-espera.svg` |
| `hospital-recepcion.svg`, `hospital-pasillo.svg`, etc. (como nombre de archivo) | **No existen** bajo `public/`; solo existen sin prefijo `hospital-` |

---

## Código exacto: instanciación y carga de imágenes

### Lista de URLs hospital en `AssetLoader`

```12:37:frontend/src/app/estudiante/simulacion-play/engine/asset-loader.ts
const HOSPITAL_ZONES = [
  'recepcion',
  'sala-espera',
  'pasillo',
  'enfermeria',
  'uci',
  'archivo',
] as const;

// ...

  async loadAll(): Promise<void> {
    const urls = [
      ...Object.values(ASSET_URLS),
      ...HOSPITAL_ZONES.map((z) => this.zoneUrl('hospital', z)),
      ...COMISARIA_ZONES.map((z) => this.zoneUrl('comisaria', z)),
    ];
```

### Construcción de URL y resolución desde caché

```59:64:frontend/src/app/estudiante/simulacion-play/engine/asset-loader.ts
  zoneUrl(sceneId: SceneId, tileId: string): string {
    return `${BASE}/${sceneId}/zones/${tileId}.svg`;
  }

  zoneFor(sceneId: SceneId, tileId: string): HTMLImageElement | null {
    return this.get(this.zoneUrl(sceneId, tileId));
```

### Instanciación `HTMLImageElement` y asignación `src`

```81:93:frontend/src/app/estudiante/simulacion-play/engine/asset-loader.ts
  private loadImage(url: string): Promise<HTMLImageElement> {
    const cached = this.cache.get(url);
    if (cached) return Promise.resolve(cached);

    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        this.cache.set(url, img);
        resolve(img);
      };
      img.onerror = () => reject(new Error(`No se pudo cargar: ${url}`));
      img.src = url;
    });
  }
```

### Quién invoca la carga al iniciar el juego

```56:63:frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts
  async prepareAssets(): Promise<void> {
    try {
      await this.assets.loadAll();
    } catch (e) {
      console.warn('[PlaySceneEngine] Carga parcial de assets', e);
    }
    this.renderer = new SceneRenderer(this.assets);
    this.assetsReady = true;
```

```490:497:frontend/src/app/estudiante/simulacion-play/simulacion-play.component.ts
    this.engine = new PlaySceneEngine(canvas, this.sceneConfig(), {
      onInteract: (t) => this.onInteract(t),
    });
    void this.engine
      .prepareAssets()
      .then(() => {
        this.engine?.resize();
        this.engine?.start();
```

### Escena hospital activa por defecto

```184:186:frontend/src/app/estudiante/simulacion-play/simulacion-play.component.ts
  readonly sceneConfig = computed<PlaySceneConfig>(() =>
    this.activeSceneId() === 'comisaria' ? COMISARIA_SCENE : HOSPITAL_SCENE,
  );
```

```12:13:frontend/src/app/estudiante/simulacion-play/data/hospital-scene.content.ts
export const HOSPITAL_SCENE: PlaySceneConfig = {
  id: 'hospital',
```

Ejemplo de enlace zona → `tileId` (recepción):

```22:31:frontend/src/app/estudiante/simulacion-play/data/hospital-scene.content.ts
    {
      id: 'recepcion',
      label: 'Recepción',
      tileId: 'recepcion',
      x: 40,
      y: 40,
      w: 300,
      h: 220,
      floor: '#e8f4f8',
      accent: '#0ea5e9',
    },
```

---

## Código exacto: dibujo en canvas

### Orquestación por frame

```149:159:frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts
  private loop = (ts: number): void => {
    const dt = Math.min(0.05, (ts - this.lastTs) / 1000);
    this.lastTs = ts;
    if (!this.paused) {
      this.player.update(dt, this.scene.bounds, this.obstacles);
      updateNpcActors(this.npcActors, dt);
      this.nearest = this.findNearest();
      this.updateCamera();
      this.renderer?.tick(dt);
    }
    this.draw();
```

```247:248:frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts
    this.renderer.drawBackground(ctx, this.scene);
    this.renderer.drawZones(ctx, this.scene);
```

### Iteración de zonas y `drawImage` del SVG

```28:33:frontend/src/app/estudiante/simulacion-play/engine/scene-renderer.ts
  drawZones(ctx: CanvasRenderingContext2D, scene: PlaySceneConfig): void {
    for (const zone of scene.zones ?? []) {
      this.drawZoneTile(ctx, scene.id, zone);
    }
    this.drawDoors(ctx, scene.doors ?? []);
    this.drawWalls(ctx, scene);
```

```140:151:frontend/src/app/estudiante/simulacion-play/engine/scene-renderer.ts
  private drawZoneTile(
    ctx: CanvasRenderingContext2D,
    sceneId: PlaySceneConfig['id'],
    zone: SceneZone,
  ): void {
    const tile = this.assets.zoneFor(sceneId, zone.tileId);
    if (tile) {
      ctx.drawImage(tile, zone.x, zone.y, zone.w, zone.h);
    } else {
      ctx.fillStyle = zone.floor;
      ctx.fillRect(zone.x, zone.y, zone.w, zone.h);
    }
```

**Fallback si el SVG falla al cargar:** rectángulo con `zone.floor` (color plano). No se usa el SVG.

---

## Comisaría (misma tubería, distinto `scene.id`)

| Tile | Ruta en disco | `tileId` en `comisaria-scene.content.ts` |
|------|---------------|------------------------------------------|
| Recepción | `public/assets/simulacion/comisaria/zones/recepcion.svg` | `recepcion` |
| Secretaría | `.../secretaria.svg` | `secretaria` |
| Trabajo Social | `.../trabajo-social.svg` | `trabajo-social` |
| Protección | `.../proteccion.svg` | `proteccion` |
| Archivo | `.../archivo.svg` | `archivo` |

Carga: `COMISARIA_ZONES` en `asset-loader.ts` → mismos métodos `loadImage`, `zoneFor`, `drawZoneTile`.

Activación: `irAComisaria()` → `activeSceneId.set('comisaria')` → `engine.loadScene(COMISARIA_SCENE)` → en el siguiente `draw()`, `scene.id === 'comisaria'` y `zoneFor('comisaria', tileId)` resuelve los SVG de comisaría.

---

## Verificación en build (Docker)

Tras `npm run build -- --configuration=docker`, los SVG hospital aparecen en:

`frontend/dist/frontend/browser/assets/simulacion/hospital/zones/*.svg`

Angular copia `public/**` según `angular.json` (`input: "public"`). Las mismas URLs `/assets/simulacion/...` sirven en nginx.

---

## Cómo verificar en runtime (sin asumir)

1. Abrir `http://localhost:8080/estudiante/simulacion`, completar intro y advertencia (fase `explore`).
2. DevTools → **Network** → filtrar `zones` → deben aparecer 6 peticiones `200` a `/assets/simulacion/hospital/zones/*.svg` al cargar.
3. DevTools → **Console**:

```javascript
// Tras cargar el canvas, si hay acceso al motor no expuesto; alternativa Network es suficiente
performance.getEntriesByType('resource')
  .filter(e => e.name.includes('/hospital/zones/'))
  .map(e => ({ name: e.name, duration: e.duration }));
```

4. Si algún SVG devuelve **404**, esa zona caerá en el fallback `fillRect(zone.floor)` y **no** se verá el arte V7 en esa celda.

---

## Conclusión de auditoría

- **Los escenarios V7 hospital están conectados al juego** mediante `tileId` → `zoneUrl` → `loadImage` → `drawZoneTile` → `drawImage` en cada frame.
- **No asumir** nombres con prefijo `hospital-` ni `hospital-espera.svg`; el nombre real de sala de espera es **`sala-espera.svg`**.
- **Sí hay** una capa anterior V5 (`hospital-bg.svg` / `comisaria-bg.svg`) aún dibujada debajo, con baja opacidad.
- **No hay** función separada `renderHospital()` / `renderComisaria()`; ambas usan `SceneRenderer` + `PlaySceneEngine.draw()` según `scene.id`.
