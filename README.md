# Proyecto Nuclear · SOMA (Simulador académico de casos)

Ingeniería de Software II · Corporación Universitaria Empresarial Alexander von Humboldt · 2026-II

El repositorio está organizado **por módulos**. Cada carpeta es una aplicación que hoy se compila por separado;
la integración en una sola aplicación se hará sobre esta estructura.

| Carpeta | Módulos | Requisitos | Responsable | Tecnología | Documentación |
|---|---|---|---|---|---|
| [`modulo-1-2/`](modulo-1-2) | 1. Usuarios, autenticación y permisos | RF-01, RF-02, RF-03 | Samuel Velásquez Rojas | Java 21, Spring Boot 4.1.2, JPA, Flyway, JWT · Angular 21 | [docs/modulo-1](docs/modulo-1/README.md) |
| | 2. Creación y validación de casos | RF-05 a RF-09, RF-17 | Alejandro Ospina Rodríguez | (misma aplicación que el módulo 1) | [docs/modulo-2](docs/modulo-2/README.md) |
| [`modulo-3-4/`](modulo-3-4) | 3. Asociación académica, programación y convocatorias · 4. Participación y control de sesiones | RF-04, RF-18, RF-20 · RF-10, RF-11, RF-12, RF-19, RF-22, RF-23, RF-25, RF-28 | Peter Manolo Carvajal Lugo | Java 21, Spring Boot 4.1.2, JdbcTemplate, MySQL | [docs/modulo-3-4](docs/modulo-3-4/README.md) |
| [`modulo-5-6/`](modulo-5-6) | 5. Participaciones, evaluación, resultados y reportes · 6. Notificaciones y retroalimentación | RF-12 a RF-16, RF-24 · RF-21, RF-26, RF-27 | Sofía Yuliana Murillo Parra | Java 17, Spring Boot 3.4.5, JPA, MySQL (Docker) | [docs/modulo-5-6](docs/modulo-5-6/README.md) |

```
.
├── modulo-1-2/
│   ├── backend/      co.edu.humboldt.plataformacasos (autenticacion, autorizacion, usuarios = módulo 1; casos, rubricas = módulo 2)
│   └── frontend/     Angular: login, menú por rol, docentes y permisos (módulo 1); casos y constructor (módulo 2)
├── modulo-3-4/
│   ├── pom.xml
│   └── src/          com.example.demo (modulo3, modulo4 y paquetes compartidos)
├── modulo-5-6/
│   ├── backend/      com.unihumboldt.nuclear.evaluacion
│   ├── docker-compose.yml
│   └── .env.example
└── docs/
    ├── modulo-1/     README, FRONTEND, pruebas PT-DCP-01, evidencias y diagramas
    ├── modulo-2/
    ├── modulo-3-4/
    └── modulo-5-6/
```

## Compilar y probar

| Módulos | Comando |
|---|---|
| 1 y 2 · backend | `cd modulo-1-2/backend && mvn verify` |
| 1 y 2 · frontend | `cd modulo-1-2/frontend && npm ci && npx ng test --watch=false` |
| 3 y 4 | `cd modulo-3-4 && mvn compile` (ver pendientes en su documentación) |
| 5 y 6 | `cd modulo-5-6/backend && mvn test` (H2; `mvn verify` usa MySQL con Docker) |

## Cómo se trabaja

- `main` es la rama principal y está protegida: solo recibe cambios por **Pull Request**, que aprueba el dueño
  del repositorio (`.github/CODEOWNERS`).
- Cada integrante trabaja en su rama, creada desde `main`, y abre su Pull Request hacia `main`.
- Las credenciales no se suben: cada módulo las lee de variables de entorno (`.env` está en `.gitignore`).

## Pendiente para la integración

- Unificar versiones: los módulos 5 y 6 usan Java 17 y Spring Boot 3.4.5; el resto, Java 21 y Spring Boot 4.1.2.
- Usar una sola autenticación: el token del módulo 1 en lugar de las cabeceras `X-Docente-Id` / `X-Estudiante-Id`
  (módulos 3 y 4) y del `JWT_SECRET` propio de los módulos 5 y 6.
- Una sola base de datos y un solo esquema de usuarios, estudiantes y docentes.
- Módulos 3 y 4: falta la clase de arranque, la configuración, los scripts SQL y la clase de pruebas `Datos`.
