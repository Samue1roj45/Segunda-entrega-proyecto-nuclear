# Ecos por Pixel Streaming (el juego dentro de la página)

Con Pixel Streaming el juego corre en un servidor con GPU y el estudiante lo ve y lo maneja **dentro de la página de SOMA**:
no instala nada, no sale de la plataforma y no hace falta el enlace `ecos://`. El contrato `ecos-soma/1` (canje, eventos,
resultado) es **el mismo**: SOMA sigue calculando la nota.

```
Página de SOMA (Angular)                       Servidor de Pixel Streaming (Epic)          Unreal (Ecos)
┌──────────────────────────────┐  postMessage  ┌───────────────────────────┐  canal de    ┌──────────────┐
│ iframe del reproductor  ◄────┼──────────────►│ player.js + soma-puente.js │◄── datos ──►│ PixelStreaming│
│ panel «En vivo»              │               └───────────────────────────┘   WebRTC    │ Input/Response│
└──────────────┬───────────────┘                                                          └──────┬───────┘
               │ GET /pixel-streaming, /sesiones/{id}/avance, /mis-intentos                     │ HTTPS
               ▼                                                                                ▼
                                  Backend SOMA  (/api/ecos/v1: canje, eventos, resultado)
```

## 1. Configurar SOMA

Una sola variable en el backend (Render → servicio del backend → Environment, o `.env` en local):

```
ECOS_PIXEL_STREAMING_URL=https://<tu-servidor>/?StreamerId=DefaultStreamer&AutoConnect=true&AutoPlayVideo=true&HoveringMouse=true
```

- Solo se acepta una URL `http(s)` absoluta. Vacía = el juego se abre en el computador con `ecos://`, como antes.
- La dirección de la API que recibe el juego es la de la **misma página** (`https://<sitio de SOMA>/api/ecos/v1`), porque
  el servidor del frontend reenvía `/api` al backend. No hace falta configurar `ECOS_API_URL` para el streaming; solo en
  `localhost` se usa la que publica el backend (`ECOS_API_URL`), porque `localhost` no le sirve a un servidor remoto.
- Los enlaces de `trycloudflare.com` cambian cada vez que se reinicia el túnel: al cambiar, actualiza la variable y vuelve
  a desplegar el backend (no hace falta tocar el frontend).

## 2. Instalar el puente en el reproductor

El reproductor estándar de Epic no habla con la página que lo contiene. `pixel-streaming/soma-puente.js` lo conecta:

1. Copia `soma-puente.js` a la carpeta `www` que sirve el servidor de señalización (Wilbur/Cirrus), junto a `player.js`.
2. En `player.html` agrega, **después** de `player.js`:
   ```html
   <script defer src="player.js"></script>
   <script defer src="soma-puente.js" data-soma-origenes="https://front-lbjo.onrender.com,http://localhost:4200"></script>
   ```
   `data-soma-origenes` limita qué páginas de SOMA pueden usar el puente (vacío = la primera que salude).

Sin el puente el juego **igual se ve y se juega** dentro de SOMA, pero la página no puede pasarle el código: el panel
«En vivo» muestra el código para escribirlo en el juego, y el avance llega igual por la API (eventos del juego).

## 3. Lo que debe hacer Unreal

**Recibir la sesión** (SOMA → juego). El puente llama `emitUIInteraction` con este JSON; en Unreal llega por
`PixelStreamingInput → OnInputEvent` (o *Bind Event to On Input Event*):

```json
{ "tipo": "ecos_sesion", "sesionId": "…", "codigo": "K7Q2-9XMP", "api": "https://<backend>/api/ecos/v1" }
```

Con `codigo` y `api` el juego hace exactamente lo mismo que con `-EcosSesion` y `-EcosApi`: `POST {api}/sesiones/{codigo}/canje`.
En el sitio desplegado `api` es, por ejemplo, `https://front-lbjo.onrender.com/api/ecos/v1`.

**Informar a la página** (juego → SOMA, en vivo). Con *Send Pixel Streaming Response* se envía un texto JSON. El puente lo
reenvía a la página y el panel muestra `acto`, `punto` y `texto`/`mensaje` (cualquier otro campo sale como `clave: valor`):

```json
{ "tipo": "progreso", "acto": 3, "punto": "E1_Q2", "texto": "Decidiste hablar con la líder del equipo." }
```

Lo que el juego muestra en vivo es informativo: el estado oficial y la nota salen de SOMA (`/eventos` y `/resultado`),
y la página los vuelve a consultar cada vez que llega un mensaje del juego.

## 4. Mensajes del puente (postMessage)

| Dirección | Mensaje |
|---|---|
| SOMA → reproductor | `{ fuente: 'soma', tipo: 'hola' }` cada segundo hasta que el puente responde (máx. 15 s) |
| SOMA → reproductor | `{ fuente: 'soma', tipo: 'sesion', sesionId, codigo, api }` |
| reproductor → SOMA | `{ fuente: 'ecos-ps', tipo: 'listo', version: 1 }` |
| reproductor → SOMA | `{ fuente: 'ecos-ps', tipo: 'estado', estado: 'conectado' \| 'video' \| 'desconectado' }` |
| reproductor → SOMA | `{ fuente: 'ecos-ps', tipo: 'entregada' }` (el juego ya recibió el código) |
| reproductor → SOMA | `{ fuente: 'ecos-ps', tipo: 'juego', datos: { … } }` (lo que envió Unreal) |

Seguridad: la página solo acepta mensajes del iframe y de su origen, y solo le envía datos a ese origen. El código va por
postMessage (no en la URL, para que no quede en historiales) y sigue siendo de un solo uso con vencimiento de 30 min.

## 5. Probar sin Unreal (simulador)

`pixel-streaming/simulador/` imita el reproductor de Epic con el **mismo** `soma-puente.js` y se comporta como el juego:
canjea el código, informa seis actos y envía un resultado real a SOMA.

```powershell
python -m http.server 8090 --directory pixel-streaming
```

En `.env`: `ECOS_PIXEL_STREAMING_URL=http://localhost:8090/simulador/` y agrega `http://localhost:8090` a `CORS_ORIGENES`
(solo el simulador llama a la API desde el navegador; Unreal no necesita CORS). Reinicia el backend, entra como estudiante
a *Ecos de una decisión* y pulsa **Jugar**: el panel «En vivo» muestra la conexión, los seis actos y la nota que calcula SOMA.

## 6. API agregada

| Ruta | Quién | Respuesta |
|---|---|---|
| `GET /api/ecos/v1/pixel-streaming` | estudiante (JWT web) | `{ activo, url, origen }` |
| `GET /api/ecos/v1/sesiones/{id}/avance` | dueño de la sesión (JWT web) | `{ sesionId, estado, ultimoPunto, ultimoActo, ultimoEventoEn }` (404 si no es suya) |

## Red

WebRTC necesita STUN y, detrás de redes universitarias o NAT estrictos, un servidor TURN; sin TURN algunos estudiantes
verán la página pero no el video. El túnel de Cloudflare solo lleva la página y la señalización, no el video.
