# Módulo 1 · Usuarios, autenticación y permisos (backend)

Proyecto Nuclear · RF-01, RF-02, RF-03 · Responsable: Samuel Velásquez · Rama `feature/modulo-1`

Backend en **Java 17+ / Spring Boot 3.3**, base de datos **MySQL** (H2 en memoria para las pruebas),
migraciones con **Flyway** y seguridad sin estado con **JWT**. Esta entrega es solo backend.

## Requisitos y patrones

| Requisito | Patrón (documento de patrones del equipo) | Implementación |
|---|---|---|
| RF-01 Autenticar estudiantes | Chain of Responsibility | `compartido/validacion` (`Validador<T>`, `ValidadorBase<T>`) y `autenticacion/validacion` (4 validadores encadenados en `CadenasAutenticacionConfig`) |
| RF-02 Controlar acceso por rol | Proxy | `autorizacion/ServicioAutorizacion`, `ProxyAcceso` + `@RequierePermiso` |
| RF-03 Administrar docentes y permisos | Ninguno directo (capas), protegido por el Proxy de RF-02 | `usuarios/administracion` + `usuarios/web` |

## API

| Método y ruta | Acceso | Respuestas |
|---|---|---|
| `POST /api/auth/estudiantes/login` `{correoInstitucional, codigoAutorizacion}` | Público | 200 token · 400 formato · 401 genérico |
| `POST /api/auth/personal/login` `{correoInstitucional, clave}` | Público | 200 token · 400 · 401 genérico |
| `GET /api/sesion` | Con sesión | 200 usuario, rol y permisos vigentes · 401 |
| `GET /api/autorizacion/permisos/{permiso}` | Con sesión | 204 permitido · 403 · 401 · 400 permiso desconocido |
| `GET /api/admin/docentes` | `ADMINISTRAR_DOCENTES` | 200 · 401 · 403 |
| `POST /api/admin/docentes` `{nombreCompleto, correoInstitucional}` | `ADMINISTRAR_DOCENTES` | 201 · 400 · 409 correo duplicado · 401 · 403 |
| `PUT /api/admin/docentes/{id}/permisos` `{permisos: [...]}` | `ADMINISTRAR_DOCENTES` | 200 · 400 · 404 · 401 · 403 |

Todos los errores usan el mismo cuerpo: `{fecha, estado, error, mensaje, ruta}`.
El token va en el encabezado `Authorization: Bearer <token>`.

## Ejecutar

1. MySQL 8: crear la base `plataforma_casos` y un usuario con permisos sobre ella.
2. Variables de entorno: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRETO` (Base64, mínimo 32 bytes),
   y opcionalmente `DOMINIO_INSTITUCIONAL` y `JWT_MINUTOS`.
3. Con datos ficticios de prueba: `mvn spring-boot:run -Dspring-boot.run.profiles=pruebas` (carpeta `backend`).

Usuarios ficticios del perfil `pruebas` (no son credenciales reales):

| Correo | Rol | Ingreso |
|---|---|---|
| admin.plataforma@institucion.edu.co | Administrador | clave `Clave-Admin-01` |
| carlos.gomez@institucion.edu.co | Docente (GESTIONAR_CASOS) | clave `Clave-Docente-01` |
| laura.mejia@institucion.edu.co | Docente (sin permisos) | clave `Clave-Docente-02` |
| ana.perez@institucion.edu.co | Estudiante | código `CODIGO_VALIDO_01` |
| luis.rios@institucion.edu.co | Estudiante | código `CODIGO_VALIDO_02` |

## Pruebas

`mvn verify` (o `compilar.cmd`) ejecuta 62 pruebas, entre ellas los 18 casos del documento PT-DCP-01
(`CasosPruebaRf01Test`, `CasosPruebaRf02Test`, `CasosPruebaRf03Test`). Cada caso guarda su solicitud
y su respuesta reales en `backend/target/evidencias/<caso>.json`.

`confirmar.cmd` compila y prueba; solo si todo pasa hace el commit y lo sube.

Documentos en `docs/pruebas`: PT-DCP-01 versiones 1 a 5 (plan, ejecución por parte e informe final).

## Antipatrones evitados

- **God Object**: cada clase tiene una responsabilidad (validadores, servicio, proxy, controlador).
- **Código flecha**: las reglas de RF-01 están en eslabones de una cadena, no en `if` anidados.
- **Golden Hammer**: RF-03 no fuerza un patrón; usa capas y el Proxy de RF-02.
- **Magic strings**: roles y permisos son `enum`.
- **Seguridad solo en la interfaz**: toda verificación ocurre en el servidor.
- **Entidades expuestas**: la API usa DTOs; no se exponen hashes.
- **Credenciales en el código**: BD y clave JWT por variables de entorno; códigos y contraseñas solo como hash BCrypt.
- **Modelo anémico**: las reglas de permisos asignables viven en `Usuario`.

## Pendiente para la integración con el equipo

1. Reemplazar `VerificadorCodigoAutorizacionTemporal` (y la tabla `codigo_autorizacion_temporal`)
   por la implementación de `VerificadorCodigoAutorizacion` del Módulo 3 (RF-20).
2. Alinear el modelo (`usuario`, `usuario_permiso`) con la base de datos compartida del equipo.
3. Validar el supuesto del ingreso de docentes y administrador con contraseña (RF-01 solo cubre estudiantes).
4. Validar el catálogo de permisos y el dominio institucional real.
