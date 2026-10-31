# Informe V8 — Dirección artística y calidad visual

**Versión:** V8  
**Alcance:** Solo assets SVG de zona y ajustes de renderizado visual en canvas.  
**Sin cambios:** backend, auth, expediente, hipótesis, decisiones, APIs, lógica de juego, NPCs (comportamiento).

---

## 1. Objetivo cumplido

Reemplazar los tiles V7 por escenarios V8 con mayor densidad visual: pisos diferenciados, señalética, mobiliario integrado en el tile, puertas y divisiones reconocibles, de modo que hospital y comisaría se identifiquen sin leer etiquetas de juego.

---

## 2. Comparativo V7 vs V8

| Aspecto | V7 | V8 |
|--------|----|----|
| Composición | Bloques de color + pocos iconos | Escenas con paredes, pisos con patrón, mobiliario múltiple |
| Hospital recepción | Plano funcional | Mostrador en L, ventanilla, sillas, cartel urgencias, puerta |
| Sala espera | Genérica | Sillas en fila, mesa central, revistero, reloj, cartel silencio |
| Pasillo | Línea simple | Baldosa repetida, señales direccionales (UCI, enfermería, archivo), luces, extintor |
| Enfermería | Mínima | Dos camillas, carro medicación, monitor ECG, escritorio |
| UCI | Clara pero simple | Ambiente oscuro clínico, monitores vitales, respirador, tubos, acceso restringido |
| Archivo hospital | Estantería básica | Tres módulos de archivos, escritorio con carpeta, cartel confidencial |
| Comisaría recepción | Genérica | Faja institucional, escudo, mostrador vidrio, barrera roja |
| Secretaría | Poca definición | Dos puestos, mampara, cartelera avisos, archivadores |
| Trabajo social | Mesa simple | Mesa reunión, pizarra, estantería recursos, cartel orientación |
| Protección | Básica | Sofá, mamparas, sala segura, señal entrevista privada |
| Archivo comisaría | Similar hospital | Pasillo de estanterías + mesa consulta + expedientes |
| Capa fondo V5 | 12% opacidad | 5% (tiles V8 dominan la lectura visual) |
| Etiquetas zona | Texto sobre tile | Pastilla semitransparente para legibilidad |
| Puertas canvas | Marco fino | Marco grueso + hoja + pomo |

**Archivos V7 conservados para comparar:** `docs/v7-archive-zones/hospital/` y `docs/v7-archive-zones/comisaria/`.

**Previews V8 (copia de los assets en runtime):** `docs/v8-capturas/hospital/` y `docs/v8-capturas/comisaria/`.

---

## 3. Capturas reales del canvas

Los SVG en `docs/v8-capturas/` son el mismo arte que carga el juego; para capturas **del canvas en movimiento** (con jugador, NPCs y cámara):

1. Levantar stack: `docker-compose up --build` (o frontend en `http://localhost:8080`).
2. Iniciar sesión como estudiante y entrar a una simulación (hospital o comisaría).
3. Completar intro/advertencia hasta fase **explore**.
4. Recorrer cada zona y guardar captura de pantalla (Win+Shift+S o herramienta del navegador).
5. Nombrar sugerido: `v8-hospital-recepcion.png`, `v8-comisaria-archivo.png`, etc., en `docs/v8-capturas/screenshots/` (carpeta opcional creada por el equipo).

La cadena de dibujo no cambió: `drawBackground` → `drawZones` (tiles) → `drawFurniture` → objetos → NPCs → jugador.

---

## 4. Lista de assets reemplazados (11 tiles)

### Hospital — `frontend/public/assets/simulacion/hospital/zones/`

| Archivo | tileId en escena |
|---------|------------------|
| `recepcion.svg` | recepcion |
| `sala-espera.svg` | sala-espera |
| `pasillo.svg` | pasillo |
| `enfermeria.svg` | enfermeria |
| `uci.svg` | uci |
| `archivo.svg` | archivo |

### Comisaría — `frontend/public/assets/simulacion/comisaria/zones/`

| Archivo | tileId en escena |
|---------|------------------|
| `recepcion.svg` | recepcion |
| `secretaria.svg` | secretaria |
| `trabajo-social.svg` | trabajo-social |
| `proteccion.svg` | proteccion |
| `archivo.svg` | archivo |

**No reemplazados en V8:** `hospital-bg.svg`, `comisaria-bg.svg`, sprites de jugador/NPC/objetos (solo se redujo su peso visual del fondo global).

---

## 5. Cambios de código (solo render)

| Archivo | Cambio |
|---------|--------|
| `engine/scene-renderer.ts` | Opacidad fondo V5: 0.12 → 0.05; viñeta ligera post-tile; etiquetas con pastilla; puertas y zócalos en muros más legibles |

**Sin modificaciones:** `simulacion-play.component.ts`, `hospital-scene.content.ts`, `comisaria-scene.content.ts`, `asset-loader.ts`, servicios de entrevista/decisiones/backend.

---

## 6. Build

Ejecutar desde `frontend`:

```bash
npm run build -- --configuration=docker
```

Resultado documentado al final de esta entrega en la sección «Estado build» tras ejecutar el comando.

---

## 7. Estado build

**Exitoso** — `npm run build -- --configuration=docker` (2026-06-04). Salida: `frontend/dist/frontend`.

---

## 8. Criterios de aceptación V8

- [x] 11 SVG de zona rediseñados
- [x] Comparativo V7 vs V8 documentado
- [x] Copias en `docs/v8-capturas/`
- [x] Lista de assets reemplazados
- [x] Sin nuevas mecánicas ni cambios de backend
- [ ] Capturas PNG del canvas (manual en explore; ver sección 3)
- [x] Build docker verificado en local
