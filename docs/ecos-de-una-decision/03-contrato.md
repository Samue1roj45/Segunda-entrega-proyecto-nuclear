# Contrato Ecos ↔ SOMA (ecos-soma/1) — lado SOMA

Implementado en `backend/.../ecos/`. Rutas bajo `/api/ecos/v1`.

| Ruta | Quién | Autenticación |
|---|---|---|
| `POST /sesiones` | estudiante | JWT web (crea código `XXXX-XXXX`, vence en 30 min, devuelve enlace `ecos://iniciar?sesion=…&api=…`) |
| `GET /mis-intentos` | estudiante | JWT web |
| `POST /catalogo` | administrador | JWT web (carga el banco de preguntas normalizado; idempotente por `casoVersion`) |
| `POST /sesiones/{codigo}/canje` | juego | el código (un solo uso) → devuelve token de 6 h |
| `POST /sesiones/{id}/eventos` | juego | token del canje → 202 |
| `POST /sesiones/{id}/resultado` | juego | token del canje → 201 / 200 (repetido) / 401 / 409 / 422 |

**Regla de oro:** SOMA guarda la nota que calcula con las respuestas recibidas (puntos × crédito, `round(50·Σobtenido/Σposible)/10`); la del juego solo se guarda en `nota_juego`, y `revision=true` si difiere.
Niveles: Superior ≥ 4.5, Alto ≥ 4.0, Básico ≥ 3.0, Bajo. Criterios: fortaleza ≥ 80 %, mejora < 60 %.

Configuración (`app.ecos.*`): `ECOS_API_URL`, `ECOS_MAX_INTENTOS` (por defecto 1).

## Pendiente de decidir
- Intentos por estudiante y cuál cuenta como nota oficial (primero o mejor).
- Reanudar desde otro PC (hoy `reanudar` siempre es `false`).
- Falta el catálogo real: hay que exportar `Caso1.json` al formato normalizado de `POST /catalogo`.
- Sin límite de intentos fallidos de canje (el espacio de códigos es grande y vence a los 30 min).
