# Backend — Evaluación y resultados

Módulo Spring Boot del Integrante 4 (RF-12 a RF-27). No incluye autenticación institucional, catálogo académico real ni frontend.

## Requisitos previos

- JDK 17
- Maven 3.9+ en el `PATH`, o el Maven portable del repositorio: `tools/apache-maven-3.9.9` (esa carpeta está en `.gitignore`; si no está, usa Maven instalado)
- Docker Compose, solo si vas a levantar PostgreSQL 16
- Las pruebas **no** necesitan Docker: usan el perfil `test` con H2 en memoria

## Entorno sin secretos

1. En la **raíz del repositorio** copia la plantilla pública:

```powershell
Copy-Item .env.example .env
```

2. Completa `.env` en local. **No subas `.env`** (está en `.gitignore`).
3. No pegues contraseñas, tokens ni secretos reales en este README ni en el chat.

Variables que usa el arranque (nombres; los valores viven en tu `.env`):

- `SERVER_PORT` — por defecto `8080` (`application.yml`)
- `POSTGRES_HOST`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `POSTGRES_PORT` — puerto **JDBC del host**. En `application.yml` el valor por defecto es `5435`
- `POSTGRES_HOST_PORT` — puerto publicado por Compose hacia el contenedor (`5435:5432` si no se cambia)
- `MAIL_*` — SMTP; las pruebas mockean `JavaMailSender` y no envían correo

`JWT_SECRET` y `JWT_EXPIRATION_MS` aparecen en la plantilla y en `application.yml`, pero **JWT no está integrado**: el actor se resuelve con cabeceras `X-Rol`, `X-Usuario-Id`, `X-Estudiante-Id` y `X-Docente-Id`.

Aviso de puertos: `.env.example` trae `POSTGRES_PORT=5432` (puerto interno del contenedor). Compose publica `5435` en el host y el JDBC por defecto es `5435`. Para hablar con Docker desde la máquina, alinea `POSTGRES_PORT` con `POSTGRES_HOST_PORT` (típicamente `5435`).

## PostgreSQL (opcional para desarrollo)

Desde la raíz del repositorio:

```powershell
docker compose up -d db
```

Imagen: `postgres:16-alpine`. Healthcheck interno: `pg_isready`.

Hibernate está en `ddl-auto: none` y **no hay Flyway** en este módulo. El proceso puede arrancar, pero las operaciones JPA fallarán hasta que exista el esquema. Las pruebas no usan esta base.

## Arranque del backend

Desde la raíz:

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml spring-boot:run
```

O, con Maven en el `PATH`:

```powershell
mvn -f backend/pom.xml spring-boot:run
```

Desde `backend/`:

```powershell
mvn spring-boot:run
```

Puerto: `http://localhost:8080` salvo que cambies `SERVER_PORT`.

## Pruebas

Todas las pruebas del módulo (perfil `test`, H2, sin SMTP real):

```powershell
mvn -f backend/pom.xml test
```

Con el Maven portable:

```powershell
.\tools\apache-maven-3.9.9\bin\mvn.cmd -f backend/pom.xml test
```

Desde `backend/`:

```powershell
..\tools\apache-maven-3.9.9\bin\mvn.cmd test
```

## Estado básico del servicio

`GET /api/health` está permitido sin actor (`SecurityConfig`). Ejemplo:

```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/health -UseBasicParsing
```

Respuesta esperada (cuerpo JSON): `status` = `UP`, `modulo` = `evaluacion-resultados`, y el listado de RF del alcance. Este endpoint no comprueba PostgreSQL, JWT ni el catálogo real.
