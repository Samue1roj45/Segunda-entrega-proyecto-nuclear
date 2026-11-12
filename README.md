# Proyecto Nuclear · SOMA (Simulador académico de casos)

Ingeniería de Software II · Corporación Universitaria Empresarial Alexander von Humboldt · 2026-II

El backend es **una sola aplicación** Spring Boot (monolito modular): cada módulo conserva su carpeta, su código, sus
pruebas y su documentación, y el `pom.xml` de la raíz los compila juntos. Así los seis módulos usan la misma base de
datos, el mismo token de seguridad y se comunican por interfaces (puertos) y eventos dentro del mismo proceso.

| Carpeta | Módulo | Requisitos | Responsable | Paquete | Documentación |
|---|---|---|---|---|---|
| [`modulo-1/`](modulo-1) | 1. Usuarios, autenticación y permisos | RF-01, RF-02, RF-03 | Samuel Velásquez Rojas | `autenticacion`, `autorizacion`, `usuarios`, `compartido`, `configuracion` | [modulo-1/docs](modulo-1/docs/README.md) |
| [`modulo-2/`](modulo-2) | 2. Creación y validación de casos | RF-05 a RF-09, RF-17 | Alejandro Ospina Rodríguez | `casos`, `rubricas` | [modulo-2/docs](modulo-2/docs/README.md) |
| [`modulo-3-4/`](modulo-3-4) | 3. Programación y convocatorias · 4. Participación y control de sesiones | RF-04, RF-18, RF-19, RF-20 · RF-10, RF-11, RF-22, RF-23, RF-25, RF-28 | Peter Manolo Carvajal Lugo | `sesiones` | [modulo-3-4/docs](modulo-3-4/docs/README.md) |
| [`modulo-5-6/`](modulo-5-6) | 5. Evaluación, resultados y reportes · 6. Notificaciones y retroalimentación | RF-12 a RF-16, RF-24 · RF-21, RF-26, RF-27 | Sofía Yuliana Murillo Parra | `evaluacion` | [modulo-5-6/docs](modulo-5-6/docs/README.md) |
| [`aplicacion/`](aplicacion) | Arranque, configuración y adaptadores entre módulos | — | Integración | `integracion`, `configuracion`, `web` | este README |

Todos los paquetes cuelgan de `co.edu.humboldt.plataformacasos`.

**Tecnología (una sola versión para todos):** Java 21 · Spring Boot 4.1.2 · Spring Security con JWT (HS256) ·
Spring Data JPA y JdbcTemplate · Flyway · MySQL 8 (H2 en modo MySQL para las pruebas) · Spring Mail · OpenPDF 2.0.3 ·
Apache POI 5.4.1 · Angular 21 en los frontends.

```
.
├── pom.xml                 backend único (Java 21, Spring Boot 4.1.2)
├── Dockerfile              imagen del backend único (Maven + Java 21 → JRE 21)
├── docker-compose.yml      backend SOMA + MySQL 8 + Mailpit (correo de pruebas)
├── .env.example            variables de entorno de toda la aplicación
├── aplicacion/             PlataformaCasosApplication, application.yml, adaptadores entre módulos y prueba de punta a punta
├── modulo-1/
│   ├── backend/            autenticacion, autorizacion, usuarios, compartido, configuracion/seguridad · migraciones V1-V9
│   ├── frontend/           Angular: login, menú por rol, docentes y permisos
│   └── docs/
├── modulo-2/
│   ├── backend/            casos, rubricas · migraciones V20-V29
│   ├── frontend/           Angular: panel de casos y constructor
│   └── docs/
├── modulo-3-4/
│   ├── backend/            sesiones · migraciones V30-V39
│   └── docs/
├── modulo-5-6/
│   ├── backend/            evaluacion (participacion, resultado, reporte, notificacion) · migración V40
│   └── docs/
├── postman/                colección y entorno de Postman (75 peticiones con pruebas)
└── docs/                   PATRONES.md (patrón → clases → RF) y ANTIPATRONES.md (revisión)
```

## Cómo se comunican los módulos

```
  Módulo 1 ──token JWT + permisos──▶ todos          Módulo 2 ──casos publicados / contenido──▶ 3, 4 y 5
  Módulo 3 ──VerificadorCodigoAcceso (RF-20)──▶ 1   Módulo 4 ──regla de ingreso (RF-22)──▶ 5
  Módulo 4 ──RegistroParticipaciones──▶ 5           Módulo 3 ──eventos (convocatoria, cambio, fin)──▶ 5 y 6
```

| Quién usa | A quién | Contrato | Para qué |
|---|---|---|---|
| Todos | Módulo 1 | `ServicioAutorizacion`, `Permiso`, `@RequierePermiso` + `ProxyAcceso` | Identificar al usuario del token y exigir permisos (RF-02) |
| Módulo 1 | Módulo 3 | Puerto `VerificadorCodigoAutorizacion` → `VerificadorCodigoAcceso` | El estudiante inicia sesión con el código de su convocatoria (RF-01 + RF-20) |
| Módulos 3 y 4 | Módulo 2 | `ConsultaCasosProgramables`, `ContenidoCasoInterno` | Programar solo casos publicados; mostrar el caso sin la clave (RF-10) |
| Módulo 4 | Módulo 5 | Puerto `RegistroParticipaciones` | Guardar/cambiar respuestas, progreso y cierre por tiempo (RF-10, RF-11, RF-23) |
| Módulo 5 | Módulos 1, 2, 3 y 4 | Puertos `ActorActualResolver`, `CatalogoAcademico`, `PoliticaInicioIntento` | Actor, sesiones, autorizaciones, clave de evaluación, rúbrica y regla de ingreso |
| Módulos 5 y 6 | Módulo 3 | Eventos `EstudianteConvocadoEvento`, `SesionModificadaEvento`, `SesionFinalizadaEvento` | Correo con el código (RF-20/21), aviso de cambios (RF-21), evaluar pendientes al cerrar (RF-24) |

Los puertos del módulo 5 se conectan en `aplicacion/src/main/java/co/edu/humboldt/plataformacasos/integracion`
(`ActorDesdeSesion`, `CatalogoAcademicoIntegrado`, `PoliticaInicioDesdeSesiones`, `ObservadorSesiones`), así ningún
módulo conoce las clases internas de otro.

## Recorrido completo del API

1. **Administrador** (`POST /api/auth/personal/login`) asigna permisos a un docente: `PUT /api/admin/docentes/{id}/permisos`.
2. **Docente** crea y publica el caso (`/api/casos/...`, módulo 2) y, si quiere, sus herramientas (`/api/casos/{id}/herramientas`, RF-28).
3. Agenda la sesión y convoca estudiantes: `POST /api/sesiones` (RF-04, RF-18). Cada estudiante recibe un correo con su código.
4. Inicia la sesión: `POST /api/sesiones/{id}/iniciar` (RF-19).
5. **Estudiante** entra con su correo y el código: `POST /api/auth/estudiantes/login` (RF-01, RF-20); ve sus convocatorias (`GET /api/autorizaciones`) y si puede ingresar (`GET /api/autorizaciones/{id}/ingreso`, RF-22).
6. Inicia el intento `POST /api/participaciones/iniciar` (RF-12), ve el caso (`GET /api/participaciones/{id}/contexto`), responde o cambia respuestas (`PUT /api/participaciones/{id}/preguntas/{p}/respuesta`, RF-10/11), consulta progreso, tiempo y herramientas, y finaliza (`POST /api/participaciones/{id}/finalizar`).
7. Se calcula la nota con la rúbrica (RF-13); el estudiante la consulta en `GET /api/resultados` (RF-14).
8. **Docente** concede una repetición si hace falta (`POST /api/autorizaciones/{id}/repeticiones`, RF-25), finaliza la sesión (los intentos abiertos quedan INCOMPLETOS y los ausentes con nota 0, RF-24), consulta participaciones y descarga reportes (RF-15, RF-16), elige la nota oficial, registra retroalimentación y publica calificaciones (RF-26, RF-27).

`GET /api/salud` (público) muestra el estado del backend. Todos los errores tienen el mismo cuerpo:
`{fecha, estado, error, mensaje, ruta}`. El detalle de cada ruta está en la documentación de cada módulo.

## Ejecutar con Docker (lo más rápido)

Sólo hace falta Docker Desktop:

```bash
docker compose up -d --build      # backend en http://localhost:8080, MySQL en :3306, correos en http://localhost:8025
docker compose logs -f app        # ver el arranque
docker compose down -v            # apagar y borrar la base (la próxima vez arranca con datos semilla limpios)
```

El contenedor `app` arranca con el perfil `pruebas` (datos semilla). Con un `.env` (copiado de `.env.example`) se
cambian claves, puerto o perfil. Luego se importa `postman/SOMA.postman_collection.json` y
`postman/SOMA-local.postman_environment.json` en Postman y se ejecuta la colección en orden.

## Ejecutar sin Docker para el backend

Requisitos: JDK 21, Maven 3.9+, Docker (opcional, para MySQL y el correo de pruebas) y Node 22 para los frontends.

```bash
cp .env.example .env              # completar claves; JWT_SECRETO: openssl rand -base64 32
docker compose up -d db correo     # sólo MySQL 8 en :3306 y Mailpit en http://localhost:8025
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run -Dspring-boot.run.profiles=pruebas    # http://localhost:8080
```

El perfil `pruebas` carga datos ficticios: administrador, docentes Carlos y Laura, estudiantes Ana y Luis, un caso
publicado y una sesión SIN_INICIAR con Ana y Luis convocados (códigos `CODIGO_VALIDO_01` y `CODIGO_VALIDO_02`).
Usuarios y claves en [modulo-1/docs](modulo-1/docs/README.md#ejecutar). Flyway crea todas las tablas al arrancar; si
la base tenía un esquema anterior a la integración, créela de nuevo (vacía).

Frontends (en otra terminal): `cd modulo-1/frontend && npm ci && npm start` (o `modulo-2/frontend`). El servidor de
desarrollo envía `/api` a `localhost:8080`.

## Compilar y probar

| Qué | Comando (desde la raíz) | Pruebas |
|---|---|---|
| Todo el backend | `mvn verify` | 274 |
| Módulo 1 | `mvn test -Dtest='co.edu.humboldt.plataformacasos.autenticacion.**.*Test,co.edu.humboldt.plataformacasos.autorizacion.**.*Test,co.edu.humboldt.plataformacasos.usuarios.**.*Test,co.edu.humboldt.plataformacasos.configuracion.**.*Test,co.edu.humboldt.plataformacasos.compartido.**.*Test'` | 62 |
| Módulo 2 | `mvn test -Dtest='co.edu.humboldt.plataformacasos.casos.**.*Test'` | 88 (17 solo con MySQL) |
| Módulos 3 y 4 | `mvn test -Dtest='co.edu.humboldt.plataformacasos.sesiones.**.*Test'` | 60 |
| Módulos 5 y 6 | `mvn test -Dtest='co.edu.humboldt.plataformacasos.evaluacion.**.*Test'` | 61 |
| Punta a punta | `mvn test -Dtest='FlujoCompleto*Test,SaludControladorTest'` | 3 (1 solo con MySQL) |
| Frontends | `cd modulo-1/frontend && npm ci && npx ng test --watch=false` (igual en `modulo-2/frontend`) | 20 y 4 |

Las pruebas usan H2 en memoria. Para repetirlas sobre MySQL 8 real, con bases vacías y desechables:

```bash
export MYSQL_PRUEBAS_USUARIO=plataforma MYSQL_PRUEBAS_CLAVE=... \
       MYSQL_PRUEBAS_URL=jdbc:mysql://localhost:3306/plataforma_pruebas \
       MYSQL_FLUJO_URL=jdbc:mysql://localhost:3306/plataforma_flujo
mvn verify
```

`FlujoCompletoTest` recorre los seis módulos sin dobles de prueba: permisos, caso publicado, sesión, convocatoria
con código por correo, ingreso, respuestas, nota con rúbrica, repetición, cierre de la sesión (incompletas y
ausencias), nota oficial, retroalimentación, reporte y avisos.

## Cómo se trabaja

- `main` es la rama principal y está protegida: solo recibe cambios por **Pull Request**, que aprueba el dueño
  del repositorio (`.github/CODEOWNERS`).
- Cada integrante trabaja en su rama, creada desde `main`, y abre su Pull Request hacia `main`.
- Cada módulo agrega sus migraciones en su rango de versiones (V1-V9, V20-V29, V30-V39, V40-V49) para no chocar.
- Nombres: paquetes en español bajo `co.edu.humboldt.plataformacasos`; clases `*Controlador`, `Servicio*`,
  `*Repositorio`, `Fachada*`, `Proxy*`, `Estrategia*`; errores con las excepciones de `compartido.excepcion`.
- Las credenciales no se suben: se leen de variables de entorno (`.env` está en `.gitignore`).

## Pendiente

- Frontends de los módulos 3-4 y 5-6: no están en el repositorio.
- Unir los frontends de los módulos 1 y 2 en una sola aplicación Angular (hoy son dos proyectos con el mismo login).
- Supuestos por validar con la docente: ingreso del personal con contraseña (módulo 1), pesos de las preguntas y
  escala de la nota (hoy todas pesan 1 y la escala es el mayor rango de la rúbrica), y la regla OCL-13 (RF-28).
