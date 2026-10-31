# Experiencia inmersiva — Simulación de investigación psicosocial

## Alcance

Transformación **visual y de navegación** del frontend de simulación. Sin cambios en:

- Lógica académica, casos, diálogos, decisiones, APIs, backend
- Criterios de desbloqueo (`ExpedienteCasoService`, `InterviewService`, `ConversationService`)
- Minijuegos, hipótesis ni retroalimentación inmediata en exploración

## Layout pantalla completa (80% / 20%)

- **Columna izquierda (~80%)**: escenario inmersivo por salas (`app-immersive-scene`)
- **Columna derecha (~20%)**: registro profesional permanente (`app-expediente-caso` modo `embedded`)

## Hospital — 6 espacios

| Espacio | Asset | NPCs / objetos (IDs académicos) |
|---------|-------|--------------------------------|
| Entrada principal | `hospital/rooms/entrada.svg` | Guardia |
| Recepción | `recepcion.svg` | — |
| Sala de espera | `sala-espera.svg` | Madre, Hermano |
| Pasillo de urgencias | `pasillo.svg` | — |
| Área médica | `area-medica.svg` | Enfermería, Médico |
| Consultorio | `consultorio.svg` | Historia clínica, Protocolo EPICEE |

**Navegación:** mapa, puertas, clic en suelo, transiciones suaves. Solo NPCs de la sala actual (hotspots).

## Comisaría — 5 espacios (inmersivo)

| Espacio | Asset | NPCs / objetos |
|---------|-------|----------------|
| Recepción | `comisaria/rooms/recepcion.svg` | Secretaría |
| Sala de espera | `sala-espera.svg` | Víctima |
| Oficina de atención | `oficina-atencion.svg` | Observaciones |
| Área psicosocial | `area-psicosocial.svg` | Trabajador social, Protección |
| Despacho | `despacho.svg` | Comisario, expediente, denuncias, reportes, ruta |

El **canvas** ya no se usa en fase `explore`; hospital y comisaría comparten `app-immersive-scene`.

## Narrativa y presentación

- `data/character-presentation.ts`: nombre completo, rol, emoción en entrevistas y diálogos
- `app-investigation-progress`: entrevistas, documentos, observaciones (sin calificación)
- `app-case-timeline` en expediente: eventos solo al descubrirlos
- `AmbientAudioService`: ambiente sintético hospital / comisaría (toggle en HUD)
- Registro profesional con fuente, ubicación (sala actual), informante

## Registro profesional (panel derecho)

Secciones en exploración: Caso (con cronología), Personas, Antecedentes, Evidencias, Riesgo, Pendiente, Observaciones, Contradicciones, Hechos, Hipótesis, Timeline.

Banner: «Nueva información añadida al expediente» (sin aciertos/errores).

## Archivos principales

- `data/hospital-rooms.content.ts`, `data/comisaria-rooms.content.ts`
- `data/immersive-room.types.ts`, `data/character-presentation.ts`
- `presentation/immersive-scene.component.ts`
- `presentation/investigation-progress.component.ts`, `case-timeline.component.ts`
- `services/investigation-stats.service.ts`, `ambient-audio.service.ts`, `exploration-context.service.ts`
- `simulacion-play.component.*`

## Mejora visual V9 (solo presentación)

- Escena con capas: fondo, viñeta, suelo, iluminación, transición con cortina
- Personajes con silueta, sombra, uniforme por rol (guardia, médico, madre, etc.)
- Observaciones integradas (carteles, monitores, carpetas) sin botones flotantes
- Diálogos estilo novela visual (barras cinematográficas, retrato grande)
- Expediente tipo bitácora (líneas, margen, tipografía serif)
- SVG de 11 salas regenerados: `npm run node frontend/scripts/generate-room-svgs.mjs`

## Build

`npm run build -- --configuration=docker` — exitoso.

## Probar

1. `docker-compose up --build` → http://localhost:8080
2. Estudiante → simulación → hospital: mapa, salas, entrevistas, observaciones, expediente
3. Tras umbral hospital → comisaría con mismas mecánicas inmersivas
