# Informe HOTFIX — Login estudiante

**Fecha:** 2026-06-03  
**Alcance:** Solo flujo de acceso estudiante (sin cambios en V5/V6, escenarios, NPCs ni UX de simulación)

---

## Síntoma

1. El estudiante ingresa correo y código en `/acceso-estudiante`.
2. La API responde correctamente (tokens JWT).
3. El navegador termina en **`/auth/login`** (pantalla de docente), no en la simulación.

---

## Flujo investigado

```
/acceso-estudiante (sin guard)
  → POST /api/auth/estudiante-acceso/
  → AuthService.setSession(access, refresh, usuario)
  → Router.navigate → /estudiante/simulacion
  → authGuard (MainLayout)
  → roleGuard (rol ESTUDIANTE)
  → SimulacionPlayComponent
```

**Punto de redirección al login:** `authGuard` en `app.routes.ts` (ruta padre con `MainLayout`).

No existe `StudentGuard` en el proyecto; los guards activos son `authGuard` y `roleGuard`.

---

## Causa raíz

**Archivo:** `frontend/src/app/core/auth/auth.service.ts`

`isAuthenticated` estaba definido como **`computed()`** leyendo `localStorage` directamente. En Angular, un `computed` solo se invalida cuando cambian **signals** leídas dentro del callback; `localStorage` no es reactivo.

### Secuencia que reproduce el bug

1. El usuario abre la app en `/` o pasa por `/auth/login`.
2. `authGuard` evalúa `isAuthenticated()` → **false** (sin token).
3. Ese valor queda **cacheado** en el `computed`.
4. En `/acceso-estudiante` el estudiante autentica bien y `setSession()` guarda tokens.
5. Al navegar a `/estudiante/simulacion`, `authGuard` vuelve a leer el mismo `computed` → sigue en **false**.
6. Redirección a `/auth/login`.

No es un fallo del backend ni de V5/V6 (intro, contenido sensible, canvas). La API de acceso estudiante y los claims del JWT (`autorizacion_id`, `rol`) fueron verificados correctamente en Docker.

---

## Corrección aplicada

| Cambio | Archivo |
|--------|---------|
| `isAuthenticated` pasa de `computed` a **método** que lee `localStorage` en cada llamada | `auth.service.ts` |
| Decodificación JWT con padding Base64URL correcto | `auth.service.ts` |
| Normalización de `rol` al guardar/cargar sesión | `auth.service.ts` |
| Logs de diagnóstico `[Auth]`, `[AuthGuard]`, `[RoleGuard]` | `auth.service.ts`, guards |
| Validación de tokens antes de navegar; `loading` se apaga en éxito; logs en acceso | `estudiante-acceso.ts` |

---

## Archivos modificados

- `frontend/src/app/core/auth/auth.service.ts`
- `frontend/src/app/core/guards/auth.guard.ts`
- `frontend/src/app/core/guards/role.guard.ts`
- `frontend/src/app/estudiante/estudiante-acceso.ts`
- `docs/INFORME_HOTFIX_LOGIN_ESTUDIANTE.md`

**No modificados:** backend, simulación V5/V6, MemoriasDelParque, escenarios, NPCs.

---

## Cómo verificar en consola del navegador

Tras ingresar correo + código válidos:

```
[EstudianteAcceso] Autenticación OK { isAuthenticated: true, rol: 'ESTUDIANTE', ... }
[Auth] setSession { hasAccessToken: true, accessExpired: false, rol: 'ESTUDIANTE', ... }
```

No debe aparecer:

```
[AuthGuard] Acceso denegado → /auth/login
```

---

## Despliegue

Reconstruir el frontend:

```bash
docker compose up --build -d frontend
```

O build local:

```bash
cd frontend && npm run build -- --configuration=docker
```

---

## Resultado esperado

```
correo + código válido
  → POST /api/auth/estudiante-acceso/ OK
  → sesión en localStorage
  → /estudiante/simulacion (intro narrativa → advertencia → simulación)
```

Sin volver a `/auth/login` si la autenticación fue exitosa.
