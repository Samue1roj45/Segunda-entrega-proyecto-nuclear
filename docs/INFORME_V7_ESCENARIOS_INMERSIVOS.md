# Informe V7 — Escenarios inmersivos (solo visual)

**Fecha:** 2026-06-04  
**Alcance:** Frontend `simulacion-play` — sin backend, auth, expediente, hipótesis ni mecánicas nuevas.

---

## Objetivo cumplido

Al ver el canvas, el estudiante debe reconocer de inmediato **hospital** (urgencias, señalización clínica) o **comisaría** (oficinas institucionales, archivadores) sin depender solo de bloques de color.

---

## Hospital — 6 áreas con SVG propio

| Área | Tile SVG | Elementos visibles |
|------|----------|-------------------|
| Recepción | `hospital/zones/recepcion.svg` | Mostrador, ventanilla, sillas, cartel URG |
| Sala de espera | `sala-espera.svg` | Sillas, reloj, cartel silencio |
| Pasillo principal | `pasillo.svg` | Señalética a UCI, enfermería, archivo |
| Enfermería | `enfermeria.svg` | Poste, suministros, cruz médica |
| UCI | `uci.svg` | Camillas, monitor, código urgencias |
| Archivo clínico | `archivo.svg` | Archivadores, historias, EPICEE |

Puertas dibujadas en canvas (`doors[]`) + paredes con textura.

---

## Comisaría — 5 áreas con SVG propio

| Área | Tile SVG | Elementos visibles |
|------|----------|-------------------|
| Recepción | `comisaria/zones/recepcion.svg` | Mostrador, oficina comisario |
| Secretaría | `secretaria.svg` | Escritorios, radicados |
| Trabajo Social | `trabajo-social.svg` | Salas de entrevista, red de apoyo |
| Protección | `proteccion.svg` | Escudo, medidas, ruta |
| Archivo | `archivo.svg` | Archivadores, denuncias, reportes |

Layout actualizado: recepción y secretaría son zonas separadas (antes compartidas).

---

## NPCs — comportamiento básico (V7)

- **Idle:** balanceo y sombra animada.
- **Mirada:** ojos según dirección (`facing`: up/down/left/right).
- **Patrulla:** recorridos cortos entre waypoints con pausas (`patrol[]` en cada NPC).
- Posiciones de interacción siguen al actor en movimiento (sin cambiar APIs de juego).

Archivo: `engine/npc-actor.ts`

---

## Capturas por área

Previews estáticas de cada tile (abrir en navegador o visor SVG):

`docs/v7-capturas/`

- `hospital-recepcion.svg` … `hospital-archivo.svg`
- `comisaria-recepcion.svg` … `comisaria-archivo.svg`

Capturas in-game: en la simulación, mover al jugador a cada zona y usar captura de pantalla del canvas (F12 en `http://localhost:8080/estudiante/simulacion`).

---

## Archivos modificados / creados

### Motor y datos
- `frontend/src/app/estudiante/simulacion-play/data/scene-types.ts`
- `frontend/src/app/estudiante/simulacion-play/data/hospital-scene.content.ts`
- `frontend/src/app/estudiante/simulacion-play/data/comisaria-scene.content.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/asset-loader.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/scene-renderer.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/play-scene.engine.ts`
- `frontend/src/app/estudiante/simulacion-play/engine/npc-actor.ts` *(nuevo)*

### Assets SVG (nuevos)
- `frontend/public/assets/simulacion/hospital/zones/*.svg` (6)
- `frontend/public/assets/simulacion/comisaria/zones/*.svg` (5)

### Documentación
- `docs/INFORME_V7_ESCENARIOS_INMERSIVOS.md`
- `docs/v7-capturas/*.svg` (11 previews)

---

## Build

```bash
cd frontend && npm run build -- --configuration=docker
```

**Resultado:** exitoso (2026-06-04).

---

## No modificado

Backend, autenticación, expediente, hipótesis, entrevistas, documentos, DecisionBridge, MemoriasDelParque.
