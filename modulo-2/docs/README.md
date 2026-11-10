# Módulo 2 · Creación y validación de casos académicos

Responsable: Alejandro Ospina Rodríguez · Rama `feature/alejo-creacion-validacion-casos` · Base: `feature/modulo-1`

Permite que un docente con el permiso `GESTIONAR_CASOS` construya casos de estudio con escenarios, preguntas, opciones justificadas y rúbrica, los guarde como borrador y los publique solo si cumplen las reglas del DSL-CEPS.

## Requisitos

| RF | Implementación |
|---|---|
| RF-05 Estructura académica del caso | `CasoEstudio`, `GestionCasos`, `ServicioCasos`, `CasoControlador` |
| RF-06 Contextos y preguntas por escenario | `Escenario`, `Pregunta`, `GestionEscenarios`, `GestionPreguntas` y sus controladores |
| RF-07 Preguntas y respuestas | `Respuesta`, `GestionRespuestas`, `RespuestaControlador` |
| RF-08 Justificaciones | `Respuesta.justificar`, endpoints `/justificacion` |
| RF-09 Validación, publicación y archivo | `ValidadorDslCeps`, `Regla*`, `FachadaPublicacionCasos`, `EstadoCaso` |
| RF-17 Rúbricas y criterios | `Rubrica`, `CriterioEvaluacion`, `GestionRubricas`, `RubricaControlador` |

## Tecnologías

Las mismas del módulo 1, sobre el que se construye: Java 21, Spring Boot 4.1.2 (versión del equipo desde la integración en `main`), Spring Data JPA, Flyway, Spring Security con JWT, MySQL 8 y H2 para pruebas. El frontend usa Angular 21.

## Componentes

| Paquete | Responsabilidad |
|---|---|
| `casos/dominio` | Entidades del agregado y reglas de estado y de dominio de valores |
| `casos/repositorio` | Acceso a datos y proyecciones de listado |
| `casos/servicio` | Interfaces protegidas, servicios, fachada de publicación y control de pertenencia |
| `casos/validacion` | Cadena de reglas OCL e informe de validación |
| `casos/dsl` | Builder, importación y exportación del DSL-CEPS |
| `casos/integracion` | Contratos para los módulos 3 y 4 |
| `casos/web` | Controladores REST y respuesta 422 |
| `rubricas/servicio`, `rubricas/web` | RF-17 |
| `modulo-2/frontend/src/app` | Inicio de sesión, panel de casos y constructor por secciones |

### Copia de soporte del módulo 1

Para que el módulo 2 compile y se pruebe solo, su carpeta lleva una copia mínima de la seguridad del módulo 1:
`autenticacion` (ingreso y token), `autorizacion` (`ServicioAutorizacion`, `ProxyAcceso`, `@RequierePermiso`),
`usuarios/dominio` y `usuarios/repositorio`, `compartido` y `configuracion/seguridad`, con las migraciones `V1` a `V3`
y sus datos ficticios. No incluye la administración de docentes (RF-03) ni las pruebas del módulo 1. La versión
oficial de ese código está en `modulo-1/`; al integrar los módulos en una sola aplicación esta copia se elimina.

## API

Todas las rutas exigen `Authorization: Bearer <token>` del módulo 1. Las de `/api/casos` exigen `GESTIONAR_CASOS` y que el caso sea del docente.

| Método y ruta | Descripción |
|---|---|
| `GET /api/casos?estado=` | Lista los casos del docente |
| `POST /api/casos` | Crea un borrador |
| `GET`, `PUT /api/casos/{id}` | Consulta y edita |
| `POST /api/casos/importacion-dsl` | Crea un borrador desde un documento DSL-CEPS |
| `GET /api/casos/{id}/dsl` | Exporta en DSL-CEPS, sin el estado |
| `POST /api/casos/{id}/escenarios` | Agrega un escenario |
| `GET`, `PUT`, `DELETE /api/casos/{id}/escenarios/{e}` | Consulta, edita y elimina |
| `POST /api/casos/{id}/escenarios/{e}/preguntas` | Agrega una pregunta |
| `GET`, `PUT`, `DELETE /api/casos/{id}/preguntas/{p}` | Consulta, edita y elimina |
| `POST /api/casos/{id}/preguntas/{p}/respuestas` | Agrega una opción |
| `PUT /api/casos/{id}/preguntas/{p}/respuesta-correcta` | Deja una sola opción correcta |
| `GET`, `PUT`, `DELETE /api/casos/{id}/respuestas/{r}` | Consulta, edita y elimina |
| `GET`, `PUT /api/casos/{id}/respuestas/{r}/justificacion` | Consulta y registra la justificación |
| `GET`, `PUT`, `DELETE /api/casos/{id}/rubrica` | Consulta, crea o actualiza y retira |
| `POST /api/casos/{id}/rubrica/criterios` | Agrega un criterio |
| `PUT`, `DELETE /api/casos/{id}/rubrica/criterios/{c}` | Edita y elimina |
| `GET /api/casos/{id}/validacion` | Informe de reglas, sin cambiar el estado |
| `POST /api/casos/{id}/publicacion` | Publica o responde 422 con los incumplimientos |
| `POST /api/casos/{id}/archivo` | Archiva un caso publicado |
| `GET /api/integracion/casos/publicados` | Módulo 3 · `PROGRAMAR_SESIONES` |
| `GET /api/integracion/casos/{id}/programacion` | Módulo 3 · `PROGRAMAR_SESIONES` |
| `GET /api/integracion/casos/{id}/rubrica` | Módulo 4 de evaluación · `CONSULTAR_PARTICIPACIONES` |

Errores: 400 valor inválido, 401 sin sesión, 403 sin permiso o caso ajeno, 404 inexistente o de otro caso, 409 estado que no permite la operación, 422 reglas incumplidas al publicar.

## Modelo de datos

Migraciones `V20` a `V24` en `modulo-2/backend/src/main/resources/db/migration`. La numeración desde V20 evita chocar con otros módulos.

`casos_estudio` 1—N `escenarios` 1—N `preguntas` 1—N `respuestas`; `casos_estudio` 1—0..1 `rubricas` 1—N `criterios_evaluacion`. La base garantiza una rúbrica por caso, el porcentaje entre 0 y 100, `rango_min < rango_max` y los estados válidos.

## Reglas OCL

Se evalúan al validar o publicar; un borrador se puede guardar incompleto.

| Regla | Clase | Regla | Clase |
|---|---|---|---|
| OCL-01 | `ReglaNombreCaso` | OCL-07 | `ReglaUnicaCorrecta` |
| OCL-02 | `ReglaTiempoEstimado` | OCL-08 | `ReglaCodigosUnicos` |
| OCL-03 | `ReglaCasoConEscenarios` | OCL-09 | `ReglaRubricaAsociada` |
| OCL-04 | `ReglaEscenarioCompleto` | OCL-10 | `ReglaRubricaConCriterios` |
| OCL-05 | `ReglaMinimoRespuestas` | OCL-11 | `ReglaPorcentajeRespuesta` |
| OCL-06 | `ReglaRespuestaJustificada` | OCL-12 | `ReglaCriterioValido` |

OCL-13 no está implementada porque depende de RF-28, que no pertenece a este módulo.

## Patrones

| Patrón | Dónde | Problema que resuelve |
|---|---|---|
| Chain of Responsibility | `ReglaOcl` y `Regla*`, sobre `ValidadorBase` del módulo 1 | Evaluar reglas independientes y agregar otras sin cambiar las existentes. Cada eslabón acumula en vez de cortar, porque RF-09 exige informar todas |
| Facade | `FachadaPublicacionCasos` | El controlador publica sin conocer el orden de acceso, validación y cambio de estado |
| Builder | `CasoEstudioBuilder`, con `ImportadorDsl` como director | Armar por pasos un caso con partes anidadas |
| Proxy | `ProxyAcceso` del módulo 1, en `ConfiguracionServiciosCasos` | Exigir el permiso antes de ejecutar cada servicio |
| Repository | `CasoEstudioRepositorio` | Aislar el acceso a datos |

No se aplicaron Strategy, porque las reglas no son algoritmos intercambiables, ni la jerarquía de State: con tres estados y dos transiciones basta el enum `EstadoCaso`.

Antipatrones evitados: God Class (un servicio por responsabilidad), modelo anémico (las reglas viven en las entidades), magic numbers (`LimitesDsl`), validación solo en la interfaz (todo se comprueba en el servidor) y entidades expuestas (la API usa DTO).

## Integración

- **Módulo 1:** reutiliza JWT, `ServicioAutorizacion`, `ProxyAcceso`, `Validador<T>`, excepciones y `ManejadorErroresApi`. No se modificó ningún archivo suyo.
- **Módulo 3:** `ConsultaCasosProgramables` y `ContenidoCasoInterno.contenidoSinClaves`, cuyo tipo no contiene clave, porcentaje ni justificación.
- **Evaluación:** `ConsultaRubricasEvaluacion` y `ContenidoCasoInterno.claveDeEvaluacion`.

`ContenidoCasoInterno` no tiene endpoint a propósito: quien lo llame debe verificar antes la convocatoria del estudiante.

## Decisiones pendientes

1. **Integración de backends.** Los módulos del equipo usan versiones y persistencia distintas, y los módulos 3 y 4 esperan procedimientos almacenados y tablas (`sp_publicar_caso`, `versiones_dsl_caso`, columnas como `id_docente_creador`) cuyo script no está versionado. Este módulo no los implementa.
2. **Identificadores.** El documento propone UUID; el código del equipo usa `Long`, y este módulo también.
3. **Campos de otros módulos.** `peso` de pregunta, `escalaMaxima` y `nota_maxima` de rúbrica, y `peso_porcentaje` de criterio, los usan otros módulos pero los documentos no los definen, por lo que no se agregaron.
4. **Enunciado y texto vacíos.** Ninguna regla OCL los exige, de modo que un caso puede publicarse con ellos vacíos. Se debe decidir con la docente si se agrega una regla.
5. **Corregir un caso publicado.** Es de solo lectura y los documentos no definen cómo corregirlo.
6. **RF-28.** Las herramientas no se almacenan; la importación avisa que las omite y OCL-13 sigue pendiente.
7. **Importación y exportación DSL.** No están en los RF; se agregaron como uso del Builder y se pueden retirar si el equipo lo decide.

## Ejecutar

Requisitos: JDK 21, Maven, Node 22 y MySQL 8.

```bash
# Backend: crear la base plataforma_casos y definir DB_URL, DB_USER, DB_PASSWORD y JWT_SECRETO
cd modulo-2/backend
mvn spring-boot:run -Dspring-boot.run.profiles=pruebas

# Frontend
cd modulo-2/frontend
npm install
npm start    # http://localhost:4200
```

Usuario ficticio con permiso: `carlos.gomez@institucion.edu.co` / `Clave-Docente-01`. El servidor de desarrollo envía `/api` a `localhost:8080`. La configuración para producción queda pendiente.

## Pruebas

```bash
cd modulo-2/backend && mvn clean test     # H2; omite la clase de MySQL
cd modulo-2/frontend && npx ng test --watch=false
```

Para incluir MySQL real, sobre una base vacía y desechable, se definen `MYSQL_PRUEBAS_URL`, `MYSQL_PRUEBAS_USUARIO` y `MYSQL_PRUEBAS_CLAVE`.

| Clase | Pruebas | Tipo |
|---|---|---|
| `ValidadorDslCepsTest` | 29 | Unitaria |
| `CasoEstudioTest` | 20 | Unitaria |
| `FachadaPublicacionCasosTest` | 5 | Unitaria con simulación del acceso |
| `CasosApiIntegracionTest` | 17 | Integración sobre H2 |
| `CasosApiMysqlTest` | 17 | La misma integración sobre MySQL 8 |
| `core.spec.ts` | 4 | Frontend |
