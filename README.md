# Módulo 1 · Usuarios, autenticación y permisos — Entrega 1 (base técnica)

Proyecto Nuclear · RF-01, RF-02, RF-03 · Responsable: Samuel Velásquez

## Qué incluye esta entrega

| Parte | Contenido |
|---|---|
| `backend/` | Proyecto Spring Boot 3.3 (Java 17, Maven) con el modelo de datos del módulo, la migración MySQL (Flyway), los datos ficticios de prueba y la base genérica de Chain of Responsibility. |
| `frontend/src/app/core/modelos/` | Modelos TypeScript de rol, permiso y sesión para copiar en el proyecto Angular. |

La lógica de RF-01 (login), RF-02 (Proxy de acceso) y RF-03 (CRUD de docentes) llega en las entregas 2, 3 y 4.

## Backend

1. Crear la base de datos en MySQL 8:
   ```sql
   CREATE DATABASE plataforma_casos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'plataforma'@'localhost' IDENTIFIED BY '<tu-clave>';
   GRANT ALL ON plataforma_casos.* TO 'plataforma'@'localhost';
   ```
2. Definir las variables de entorno (en IntelliJ: *Run Configuration → Environment variables*):
   `DB_URL`, `DB_USER`, `DB_PASSWORD` y, si aplica, `DOMINIO_INSTITUCIONAL`.
3. Compilar y probar: `mvn verify`
4. Ejecutar con los datos ficticios de los casos de prueba: `mvn spring-boot:run -Dspring-boot.run.profiles=pruebas`

Flyway crea las tablas al arrancar. Hibernate solo **valida** el esquema (`ddl-auto: validate`).

## Frontend

El proyecto Angular se genera con el CLI (no se incluyen archivos generados a mano):

```bash
npx @angular/cli@18 new frontend --routing --style=scss --standalone --skip-git
```

Luego copiar `frontend/src/app/core/modelos/` dentro de `frontend/src/app/core/`.

## Patrones (según el documento de análisis de patrones)

| Requisito | Patrón | Dónde |
|---|---|---|
| RF-01 | Chain of Responsibility | `compartido/validacion` (`Validador<T>`, `ValidadorBase<T>`, `ResultadoValidacion`). Genérico para que lo reutilicen RF-09, RF-20, RF-22 y RF-25. |
| RF-02 | Proxy | Entrega 3 (`ServicioAutorizacion`, proxies por servicio). |
| RF-03 | Ninguno directo | Capas: controlador → servicio → repositorio, con DTOs. |

## Antipatrones evitados

- **Magic strings**: roles y permisos son `enum` en backend y frontend.
- **Modelo anémico**: las reglas de permisos están en `Usuario` (`asignarPermisos`, `permisosEfectivos`), no en servicios sueltos.
- **Credenciales en el código**: la conexión a la BD se lee de variables de entorno.
- **Acoplamiento entre módulos**: el Módulo 1 solo conoce la interfaz `VerificadorCodigoAutorizacion`; el Módulo 3 la implementa.
- **Esquema generado por el ORM**: Flyway versiona el esquema; Hibernate solo valida.

## Puntos de coordinación con el equipo

1. `ResultadoValidacion` unifica los diagramas 6.1 (`aprobado`, `mensaje`) y 6.2 (`valido`, `observaciones`). Todos deben usar esta clase.
2. El Módulo 3 debe implementar `VerificadorCodigoAutorizacion` (RF-20).
3. RF-01 solo define el ingreso de estudiantes. Falta acordar cómo inician sesión los docentes y el administrador.
4. El catálogo de permisos (`Permiso`) es una propuesta derivada de los requisitos. Debe validarse con el equipo.
