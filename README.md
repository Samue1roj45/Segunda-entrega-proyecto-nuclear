# Proyecto Nuclear · SOMA (Simulador académico de casos)

Ingeniería de Software II · Corporación Universitaria Empresarial Alexander von Humboldt · 2026-II

El repositorio está organizado **por módulos**. Cada carpeta es una aplicación independiente, con su propio código,
sus pruebas y su documentación (`docs/` dentro de cada módulo). Los módulos 3 y 4, y los módulos 5 y 6, comparten carpeta.

| Carpeta | Módulo | Requisitos | Responsable | Tecnología | Documentación |
|---|---|---|---|---|---|
| [`modulo-1/`](modulo-1) | 1. Usuarios, autenticación y permisos | RF-01, RF-02, RF-03 | Samuel Velásquez Rojas | Java 21, Spring Boot 4.1.2, JPA, Flyway, JWT · Angular 21 | [modulo-1/docs](modulo-1/docs/README.md) |
| [`modulo-2/`](modulo-2) | 2. Creación y validación de casos | RF-05 a RF-09, RF-17 | Alejandro Ospina Rodríguez | Java 21, Spring Boot 4.1.2, JPA, Flyway · Angular 21 | [modulo-2/docs](modulo-2/docs/README.md) |
| [`modulo-3-4/`](modulo-3-4) | 3. Asociación académica, programación y convocatorias · 4. Participación y control de sesiones | RF-04, RF-18, RF-20 · RF-10, RF-11, RF-12, RF-19, RF-22, RF-23, RF-25, RF-28 | Peter Manolo Carvajal Lugo | Java 21, Spring Boot 4.1.2, JdbcTemplate, MySQL | [modulo-3-4/docs](modulo-3-4/docs/README.md) |
| [`modulo-5-6/`](modulo-5-6) | 5. Participaciones, evaluación, resultados y reportes · 6. Notificaciones y retroalimentación | RF-12 a RF-16, RF-24 · RF-21, RF-26, RF-27 | Sofía Yuliana Murillo Parra | Java 17, Spring Boot 3.4.5, JPA, MySQL (Docker) | [modulo-5-6/docs](modulo-5-6/docs/README.md) |

```
.
├── modulo-1/
│   ├── backend/      co.edu.humboldt.plataformacasos: autenticacion, autorizacion, usuarios
│   ├── frontend/     Angular: login, menú por rol, docentes y permisos
│   └── docs/         README, FRONTEND, pruebas PT-DCP-01, evidencias y diagramas
├── modulo-2/
│   ├── backend/      co.edu.humboldt.plataformacasos: casos, rubricas (+ copia de soporte de la seguridad del módulo 1)
│   ├── frontend/     Angular: panel de casos y constructor
│   └── docs/
├── modulo-3-4/
│   ├── pom.xml
│   ├── src/          com.example.demo (modulo3, modulo4 y paquetes compartidos)
│   └── docs/
└── modulo-5-6/
    ├── backend/      com.unihumboldt.nuclear.evaluacion
    ├── docker-compose.yml
    ├── .env.example
    └── docs/
```

## Cómo se relacionan los módulos

- **Módulo 1 → todos:** emite el token JWT y define los permisos (`Permiso`). Los demás módulos deben usar ese token.
- **Módulo 2:** protege sus servicios con el Proxy y los permisos del módulo 1. Para funcionar solo, su carpeta lleva una
  copia mínima de esa seguridad (ver [modulo-2/docs](modulo-2/docs/README.md#copia-de-soporte-del-módulo-1)).
  Ofrece a los módulos 3 y 4 los contratos `ConsultaCasosProgramables` y `ConsultaRubricasEvaluacion`.
- **Módulo 3 → Módulo 1:** el código de autorización (RF-20) entra al login del módulo 1 por el puerto
  `VerificadorCodigoAutorizacion`; mientras tanto, el módulo 1 usa una implementación temporal.
- **Módulos 5 y 6:** reciben prácticas, estudiantes, preguntas y rúbricas por el puerto `CatalogoAcademico`
  (hoy una implementación temporal en memoria).

## Compilar y probar

| Módulo | Comando |
|---|---|
| 1 · backend | `cd modulo-1/backend && mvn verify` |
| 1 · frontend | `cd modulo-1/frontend && npm ci && npx ng test --watch=false` |
| 2 · backend | `cd modulo-2/backend && mvn verify` |
| 2 · frontend | `cd modulo-2/frontend && npm ci && npx ng test --watch=false` |
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
- Unir los módulos 1 y 2 en una sola aplicación y eliminar la copia de soporte del módulo 2.
- Una sola base de datos y un solo esquema de usuarios, estudiantes y docentes.
- Módulos 3 y 4: falta la clase de arranque, la configuración, los scripts SQL y la clase de pruebas `Datos`.
