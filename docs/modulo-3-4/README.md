# Módulos 3 y 4 · Programación de sesiones y participación

Responsable: Peter Manolo Carvajal Lugo · Ramas `develop/feature/peter/modulo3` y `develop/feature/peter/modulo4`
(unidas en `main` dentro de `modulo-3-4/`).

| Módulo | Requisitos |
|---|---|
| 3. Asociación académica, programación y convocatorias | RF-04, RF-18, RF-20 |
| 4. Participación y control de sesiones | RF-10, RF-11, RF-12 (consulta), RF-19, RF-22, RF-23, RF-25, RF-28 |

## Estructura (`modulo-3-4/src/main/java/com/example/demo`)

| Paquete | Contenido |
|---|---|
| `modulo3/controller`, `modulo4/controller` | Controladores REST (`/api/sesiones`, `/api/acceso`, `/api/autorizaciones`, `/api/participaciones/...`) |
| `facade`, `service`, `service/validacion`, `service/repeticion` | Lógica de negocio, Facade y cadenas de validación (Chain of Responsibility) |
| `state`, `politica`, `proxy`, `event` | State (RF-19), Strategy (RF-22), Proxy (RF-10, RF-11) y Observer (RF-11) |
| `repository` | Acceso a datos con JdbcTemplate y procedimientos almacenados |
| `scheduler`, `configuracion`, `notificacion` | Tareas programadas, CORS, política de ingreso y avisos |
| `model`, `dto`, `exception` | Objetos del dominio, datos del API y errores |

Pruebas en `src/test/java/com/example/demo/modulo3` y `modulo4`, separadas en `positivas` y `negativas`.

## Compilar

```bash
cd modulo-3-4
mvn compile
```

El `pom.xml` (Java 21 y Spring Boot 4.1.2) se agregó al organizar el repositorio, porque la rama solo tenía `src/`.

## Pendiente de subir (Peter)

- La clase de arranque con `@SpringBootApplication` y `application.yml`.
- Los scripts SQL de la base (tablas, triggers y procedimientos almacenados).
- La clase de apoyo de pruebas `com.example.demo.test.Datos`: sin ella las pruebas no compilan, por eso el
  `pom.xml` las omite por defecto. Cuando exista: `mvn verify -Dmaven.test.skip=false`.
