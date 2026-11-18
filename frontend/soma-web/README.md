# SOMA · frontend web

Interfaz única de SOMA (Angular 21): inicio de sesión y panel interior con el estilo visual de SOMA
(lila, cristal y niebla). Consume el backend único (`/api`) y muestra a cada rol solo lo que sus permisos
le permiten (la autorización real la hace el servidor).

## Ejecutar

Con el backend en `http://localhost:8080` (por ejemplo `docker compose up -d` desde la raíz):

```bash
cd frontend/soma-web
npm ci
npm start          # http://localhost:4200 (las peticiones /api van al backend por proxy.conf.json)
```

Usuarios de prueba (perfil `pruebas`):

| Rol | Correo | Contraseña o código |
|---|---|---|
| Docente | carlos.gomez@institucion.edu.co | Clave-Docente-01 |
| Administrador | admin.plataforma@institucion.edu.co | Clave-Admin-01 |
| Estudiante | ana.perez@institucion.edu.co | CODIGO_VALIDO_01 (botón «¿Eres estudiante?») |

## Estructura

```
src/app/
├── compartido/   logo.ts (isotipo SVG) · fondo.ts (escena de fondo en SVG)
├── core/         sesion.service.ts (login, «Recordarme») · acceso.ts (token y guardias) · soma-api.service.ts
└── paginas/      login · panel (menú lateral y cabecera) · inicio (resumen, sesiones, casos, participaciones, resultados)
```

Los botones de Google, Microsoft y Apple, «¿Olvidaste tu contraseña?» y «Regístrate» muestran un aviso:
el backend aún no ofrece esas funciones.
