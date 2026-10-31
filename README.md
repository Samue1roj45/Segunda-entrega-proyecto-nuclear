# Simulador de Decisiones Psicosociales

Aplicación web académica para digitalizar una estrategia de psicología social basada en **gamificación, juego de roles y toma de decisiones**. Permite a docentes universitarios crear casos simulados con escenarios narrativos (storytelling), preguntas, respuestas, rúbricas e integración con IA generativa, y a estudiantes participar en simulaciones que se evalúan automáticamente con retroalimentación final.

---

## Arquitectura

Arquitectura **frontend-backend separada** comunicada vía **API REST**:

```
┌─────────────────┐   HTTP/JSON   ┌─────────────────┐   SQL    ┌──────────────┐
│  Frontend       │ ◄───────────► │  Backend        │ ◄──────► │ PostgreSQL   │
│  Angular 18+    │               │  Django + DRF   │          │ (Docker)     │
│  Material       │               │  JWT + CORS     │          │              │
└─────────────────┘               └─────────────────┘          └──────────────┘
```

- **Backend**: Python, Django, Django REST Framework, JWT, PostgreSQL, Docker.
- **Frontend**: Angular standalone components, Angular Material, TypeScript, SCSS.
- **Base de datos**: PostgreSQL desde el inicio (nunca SQLite).
- **Contenedores**: Docker Compose levanta frontend + backend + PostgreSQL juntos.

---

## Requisitos del sistema

| Herramienta | Versión |
|---|---|
| Docker Desktop | última estable |
| Node.js | 22+ |
| npm | 10+ |
| Angular CLI | 18+ (se instala globalmente) |
| Python | no requerido localmente (corre en Docker) |

---

## Cómo levantar todo el proyecto (con Docker)

```powershell
# 1. Ir a la raíz del repositorio (donde está docker-compose.yml)
cd simulador-decisiones-psicosociales-main

# 2. Copiar plantilla de variables y completar valores
Copy-Item .env.example .env

# 3. Levantar frontend + backend + PostgreSQL
docker compose up --build -d

# 4. Crear superusuario administrador (las migraciones corren al arrancar el backend)
docker compose exec backend python manage.py createsuperuser
```

| Servicio   | URL |
|------------|-----|
| Frontend   | http://localhost:8080 |
| Backend API| http://localhost:8000/api/ |
| Healthcheck| http://localhost:8000/api/health/ |
| Admin Django | http://localhost:8080/admin/ (proxy nginx) o http://localhost:8000/admin/ |
| PostgreSQL | `localhost:5435` (usuario/clave en `.env`) |

El frontend en Docker usa nginx y envía las peticiones `/api` al backend; no hace falta `ng serve`.

## Cómo levantar solo el frontend en desarrollo local (opcional)

```powershell
cd frontend
npm install
ng serve --open
```

Frontend en `http://localhost:4200` (API en `http://localhost:8000`; requiere backend levantado con Docker o local).

---

## Estructura del repositorio

```
simulador-decisiones-psicosociales/
├── README.md
├── .gitignore
├── .env.example                    # plantilla pública (sin secretos)
├── docker-compose.yml              # frontend + backend + postgres
│
├── docs/                           # documentación técnica del proyecto
│   ├── 01-contexto-proyecto.md
│   ├── 02-requerimientos-funcionales.md
│   ├── 03-casos-de-uso.md
│   ├── 04-reglas-negocio.md
│   ├── 05-modelo-datos.md
│   ├── 06-arquitectura.md
│   ├── 07-endpoints-api.md
│   ├── 08-plan-desarrollo.md
│   └── 09-despliegue.md
│
├── backend/                        # Django + DRF
│   ├── Dockerfile
│   ├── entrypoint.sh
│   ├── requirements.txt
│   ├── manage.py
│   ├── config/                     # settings, urls, wsgi
│   └── apps/
│       ├── usuarios/               # autenticación, roles, JWT
│       ├── academico/              # estudiantes, grupos
│       ├── casos/                  # casos de estudio, storytelling
│       ├── ia_generativa/          # asistencia con IA
│       ├── importacion_documentos/ # PDF/DOCX → caso
│       ├── practicas/              # agendamiento, códigos
│       ├── participaciones/        # simulación del estudiante
│       ├── resultados/             # calificación, feedback
│       └── reportes/               # PDF/Excel, métricas admin
│
├── frontend/                       # Angular standalone
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/app/
│       ├── core/                   # interceptors, guards, auth service
│       ├── shared/                 # layout, componentes reutilizables
│       ├── auth/                   # login, registro docente, acceso estudiante
│       ├── admin/                  # dashboard administrador
│       ├── docente/                # panel docente
│       ├── estudiante/             # acceso con código
│       ├── casos/
│       ├── ia-generativa/
│       ├── importacion-documentos/
│       ├── practicas/
│       ├── participaciones/
│       ├── resultados/
│       └── reportes/
│
└── entregables/                    # material académico
    ├── diagramas/
    ├── avances/
    └── presentacion/
```

---

## Flujo de ramas y Pull Requests

```
main      ◄───── PR (versiones estables) ◄───── develop
develop   ◄───── PR (cada funcionalidad)  ◄───── feature/<modulo>
```

**Reglas:**

- Nadie trabaja directamente sobre `main`.
- Todo desarrollo sale desde `develop`.
- Cada funcionalidad vive en una rama `feature/nombre-del-modulo`.
- Los cambios se integran a `develop` mediante **Pull Request** (no `git merge` local directo).
- `main` se actualiza solo cuando `develop` tenga una versión estable verificada.
- Cada commit con mensaje claro siguiendo convención: `tipo(scope): descripción` (`feat`, `fix`, `chore`, `docs`, `refactor`, etc.).

---

## Documentación

Toda la documentación técnica vive en [`docs/`](./docs):

- [Contexto del proyecto](./docs/01-contexto-proyecto.md)
- [Requerimientos funcionales](./docs/02-requerimientos-funcionales.md)
- [Casos de uso](./docs/03-casos-de-uso.md)
- [Reglas de negocio](./docs/04-reglas-negocio.md)
- [Modelo de datos](./docs/05-modelo-datos.md)
- [Arquitectura](./docs/06-arquitectura.md)
- [Endpoints API](./docs/07-endpoints-api.md)
- [Plan de desarrollo](./docs/08-plan-desarrollo.md)
- [Despliegue](./docs/09-despliegue.md)

---

## Licencia y autoría

Proyecto académico — Universidad Alexander Von Humboldt
.
