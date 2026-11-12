# Módulo 1 · Frontend (usuarios, autenticación y permisos)

Interfaz en Angular del Módulo 1, en la carpeta `frontend/usuarios-y-permisos/`.
Consume el API del backend del módulo (`/api/auth`, `/api/sesion`, `/api/admin/docentes`).

## Pantallas

| Ruta | Pantalla | Requisito | Acceso |
|---|---|---|---|
| `/login` | Inicio de sesión: estudiante (correo + código de autorización) o personal (correo + contraseña) | RF-01 | Libre |
| `/inicio` | Menú con solo las opciones del rol y los permisos de la sesión | RF-02 | Con sesión |
| `/acceso-denegado` | Aviso cuando la sesión no tiene el permiso de la pantalla pedida | RF-02 | Libre |
| `/admin/docentes` | Crear docentes y asignar o retirar sus permisos | RF-03 | Permiso `ADMINISTRAR_DOCENTES` |

## Cómo se protege

- `SesionService` guarda la sesión que entrega el servidor (token, rol y permisos) y la descarta cuando el token vence.
- `autenticacionInterceptor` envía el token en cada petición y, si el servidor responde 401, cierra la sesión.
- `guardiaSesion` y `guardiaPermiso(permiso)` deciden qué pantallas se abren: sin sesión llevan a `/login`
  y sin el permiso a `/acceso-denegado`.
- Las guardias y el menú **solo muestran u ocultan**. La autorización real la hace el backend con el Proxy de RF-02,
  que responde 403 aunque alguien escriba la ruta a mano o llame al API directamente.

## Pruebas (Vitest con `ng test`)

| Archivo | Qué comprueba |
|---|---|
| `core/sesion-modulo1.spec.ts` | Ingreso del estudiante, rechazo del servidor, sesión vencida, guardias y envío del token |
| `paginas/login/login.spec.ts` | CP-RF01-01, 02 y 05 desde la interfaz e ingreso del personal |
| `paginas/inicio/inicio.spec.ts` | CP-RF02-01, 03 y 04 (menú por rol y permisos) y la página de acceso denegado |
| `paginas/admin-docentes/admin-docentes.spec.ts` | CP-RF03-01 a 05 desde la interfaz |

## Ejecutar

```bash
cd frontend/usuarios-y-permisos
npm ci
npm start          # http://localhost:4200, con el backend en http://localhost:8080 (proxy.conf.json)
npx ng test --watch=false
```

Usuarios de prueba (ficticios, de la semilla del backend): `ana.perez@institucion.edu.co` con el código
`CODIGO_VALIDO_01` (estudiante), `carlos.gomez@institucion.edu.co` / `Clave-Docente-01` (docente con
gestión de casos) y `admin.plataforma@institucion.edu.co` / `Clave-Admin-01` (administrador).
