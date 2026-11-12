# Módulo 1 · Usuarios, autenticación y permisos (backend)

Proyecto Nuclear · RF-01, RF-02, RF-03 · Responsable: Samuel Velásquez · Rama `feature/modulo-1`

Backend en **Java 21 / Spring Boot 4.1.2**, base de datos **MySQL** (H2 en memoria para las pruebas),
migraciones con **Flyway** y seguridad sin estado con **JWT**. El frontend del módulo está descrito en [FRONTEND.md](FRONTEND.md).

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

El módulo 1 ya no se ejecuta solo: forma parte del **backend único** (ver el [README de la raíz](../../README.md)).
Desde la raíz del repositorio, con MySQL y las variables de `.env.example` (`DB_URL`, `DB_USER`, `DB_PASSWORD`,
`JWT_SECRETO`...):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=pruebas
```

Usuarios ficticios del perfil `pruebas` (no son credenciales reales):

| Correo | Rol | Ingreso |
|---|---|---|
| admin.plataforma@institucion.edu.co | Administrador | clave `Clave-Admin-01` |
| carlos.gomez@institucion.edu.co | Docente (GESTIONAR_CASOS) | clave `Clave-Docente-01` |
| laura.mejia@institucion.edu.co | Docente (sin permisos) | clave `Clave-Docente-02` |
| ana.perez@institucion.edu.co | Estudiante | código `CODIGO_VALIDO_01` |
| luis.rios@institucion.edu.co | Estudiante | código `CODIGO_VALIDO_02` |

Los códigos de Ana y Luis pertenecen a la sesión de prueba que crea la semilla del módulo 3
(`V35_1__Sesion_prueba`): el código de acceso es de una sesión programada (RF-20), no del usuario.

## Pruebas

Desde la raíz: `mvn test -Dtest='co.edu.humboldt.plataformacasos.autenticacion.**.*Test,co.edu.humboldt.plataformacasos.autorizacion.**.*Test,co.edu.humboldt.plataformacasos.usuarios.**.*Test,co.edu.humboldt.plataformacasos.configuracion.**.*Test,co.edu.humboldt.plataformacasos.compartido.**.*Test'`
ejecuta las 62 pruebas del módulo, entre ellas los 18 casos del documento PT-DCP-01
(`CasosPruebaRf01Test`, `CasosPruebaRf02Test`, `CasosPruebaRf03Test`). Cada caso guarda su solicitud
y su respuesta reales en `target/evidencias/<caso>.json`.

Documentos en `modulo-1/docs/pruebas`: PT-DCP-01 versiones 1 a 5 (plan, ejecución por parte e informe final).

## Antipatrones evitados

- **God Object**: cada clase tiene una responsabilidad (validadores, servicio, proxy, controlador).
- **Código flecha**: las reglas de RF-01 están en eslabones de una cadena, no en `if` anidados.
- **Golden Hammer**: RF-03 no fuerza un patrón; usa capas y el Proxy de RF-02.
- **Magic strings**: roles y permisos son `enum`.
- **Seguridad solo en la interfaz**: toda verificación ocurre en el servidor.
- **Entidades expuestas**: la API usa DTOs; no se exponen hashes.
- **Credenciales en el código**: BD y clave JWT por variables de entorno; códigos y contraseñas solo como hash BCrypt.
- **Modelo anémico**: las reglas de permisos asignables viven en `Usuario`.

## Integración con los demás módulos

- **Token único:** el JWT que emite este módulo es el único de la aplicación. Los módulos 2 a 6 identifican al
  usuario con `ServicioAutorizacion` (el módulo 5 lo recibe por su puerto `ActorActualResolver`) y exigen los
  permisos de `Permiso`: `GESTIONAR_CASOS` (módulo 2 y herramientas de RF-28), `PROGRAMAR_SESIONES` (RF-04, RF-18,
  RF-19, RF-21), `AUTORIZAR_REPETICION` (RF-25), `CONSULTAR_PARTICIPACIONES` (RF-13 a RF-15, RF-24),
  `DESCARGAR_REPORTES` (RF-16), `REGISTRAR_RETROALIMENTACION` (RF-26); el estudiante usa `PARTICIPAR_EN_CASOS` y
  `CONSULTAR_RESULTADOS_PROPIOS`.
- **RF-20:** `VerificadorCodigoAutorizacion` lo implementa el módulo 3 (`VerificadorCodigoAcceso`). Se eliminaron
  `VerificadorCodigoAutorizacionTemporal`, la migración `V2` y la tabla `codigo_autorizacion_temporal`.
- **Compartido:** `ManejadorErroresApi` y `ErrorApi` son el formato de error de toda la API; se agregó
  `ReglaNegocioException` (400) para los módulos 3 a 6. La configuración de seguridad también habilita CORS
  para los frontends (`CORS_ORIGENES`).
- La clase de arranque y `application.yml` se movieron a `aplicacion/` (raíz del repositorio).

## Pendiente

1. Validar el supuesto del ingreso de docentes y administrador con contraseña (RF-01 solo cubre estudiantes).
2. Validar el catálogo de permisos y el dominio institucional real.
