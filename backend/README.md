# Backend — Evaluación y resultados

Módulo Spring Boot del Integrante 4. No incluye autenticación institucional, casos ni administración académica.

## Requisitos

- JDK 17
- Maven 3.9+ (o el Maven portable en `tools/`, si se usa en local)
- PostgreSQL 16 vía Docker Compose en la raíz del repositorio

## Arranque local (cuando exista base de datos)

```powershell
Copy-Item .env.example .env
docker compose up -d db
cd backend
mvn spring-boot:run
```

Health: `GET http://localhost:8080/api/health`
