# Onboarding de SOMA

Guías de uso para las personas que usan SOMA: una para docentes y otra para estudiantes. Están escritas para quien entra a la plataforma por primera vez y no explican código ni arquitectura.

| Archivo | Contenido |
|---|---|
| `Onboarding Docente.docx` / `.pdf` | Ingreso, menú del docente, creación y publicación de casos, estudiantes convocados, programación y control de sesiones, resultados, nota oficial, retroalimentación, repetición y recomendaciones. 29 páginas, 24 figuras. |
| `Onboarding Estudiante.docx` / `.pdf` | Ingreso con código, menú del estudiante, consulta de la sesión, desarrollo del caso, finalización, resultados, segundo intento y recomendaciones. 12 páginas, 6 figuras. |
| `capturas/docente/`, `capturas/estudiante/` | Capturas reales usadas en las guías, tomadas de la aplicación local el 22/11/2026. |

Los PDF se generaron a partir de la última versión de los Word. La tabla de contenido tiene la paginación final y se comprobó contra el PDF.

## Versión analizada

- Fecha de elaboración: 22 de noviembre de 2026.
- Repositorio: `https://github.com/Samue1roj45/Segunda-entrega-proyecto-nuclear`, rama `main`.
- Commit: `09317beb4a3e9cdb690897e6b7728f1b2c70b600` (19/11/2026, «Paginación y filtro en auditoría, índices, preferencias por usuario, modo automático, estadísticas del docente, avisos de sesión, CI con MySQL y documentación»).
- Cambios locales sin commit, que no se tocaron:
  - modificados: `README.md`, `docker-compose.yml` (agrega el servicio `web`), `iniciar-soma.bat`, `levantar-soma.bat`, `.env.example` y `frontend/soma-web/README.md`;
  - nuevos: `frontend/soma-web/Dockerfile`, `frontend/soma-web/nginx.conf` y `frontend/soma-web/.dockerignore`.
  - El código fuente de `frontend/soma-web/src` es igual al del commit; solo cambian los finales de línea (CRLF). El backend no tiene cambios.

## Fuentes revisadas

- **Frontend `frontend/soma-web`** (la interfaz que se ejecuta): `app.routes.ts`, guardas e interceptor (`core/acceso.ts`), `sesion.service.ts`, `soma-api.service.ts`, modelos (`dominio.ts`, `permiso.ts`, `rol.ts`) y todas las páginas: `login`, `panel` (menú), `inicio`, `perfil`, `casos/casos-lista`, `casos/caso-editor`, `sesiones/*`, `evaluacion/*`, `estudiante/*` y `admin/acceso-denegado`.
- **Backend `backend/src/main/java/co/edu/humboldt/plataformacasos`** (leído desde el historial de Git del commit): autenticación (mensajes, bloqueo por intentos), roles y permisos, reglas de validación de casos (OCL-01 a OCL-12), estados del caso y de la sesión, validaciones de la agenda, política de ingreso tardío, tareas de cierre automático, cadena de validación de repeticiones, fachada de participación, cálculo de nota y rúbrica, cierre de pendientes, nota oficial, notificaciones y retroalimentación.
- **Datos semilla** del perfil `pruebas`: usuarios, claves de prueba, caso publicado y sesión de prueba.
- **Documentación:** `README.md` y `docs/modulo-*.md`.
- `frontend/usuarios-y-permisos` y `frontend/casos` no se usaron, porque la aplicación que se ejecuta (`docker-compose.yml`, servicio `web`) es `frontend/soma-web`.
- El archivo `Onboarding Docente.pdf` de Descargas solo se localizó. No se abrió ni se usó como fuente.

## Procedimiento de ejecución

La aplicación ya estaba en ejecución en Docker (interfaz en `http://localhost:4200`). No se ejecutó ningún script ni comando de Docker. `levantar-soma.bat` no se debe usar para esto, porque ejecuta `docker compose down -v` y borra la base. El procedimiento sin borrado es `iniciar-soma.bat` (`docker compose up -d --build`).

Cuentas usadas (datos ficticios del perfil `pruebas`, documentados en `iniciar-soma.bat` y en las semillas): docente Carlos Gómez, administrador de prueba y estudiante Ana Pérez con su código de convocatoria. Las capturas se tomaron con el navegador integrado de la app de escritorio, a 1100 × 700 px.

## Roles revisados y cambios en la base

- **Docente:** Carlos Gómez. Originalmente solo tenía «Gestionar casos». Con autorización de la integrante, el administrador de prueba le asignó temporalmente desde la pantalla Docentes «Programar sesiones», «Consultar participaciones», «Descargar reportes», «Retroalimentar» y «Autorizar repeticiones». Al terminar las capturas se le **restauró** el permiso original («Gestionar casos») y se verificó recargando la pantalla. Los permisos de Laura Mejía no se tocaron.
- **Estudiante:** Ana Pérez.
- Por indicación de la integrante, no se crearon casos, sesiones, intentos, notas ni retroalimentaciones, y no se envió ningún formulario de datos académicos. Los diálogos (por ejemplo, Autorizar repetición) se abrieron y se cerraron con Cancelar.
- Registros que sí quedaron: los avisos de «INGRESO» que el backend guarda en cada inicio de sesión y las constancias que genera el cambio de permisos.

## Funciones comprobadas en pantalla

Ingreso de docente y de estudiante, aviso de campos vacíos, menú por permisos (docente completo y docente con solo «Gestionar casos»), Inicio, Mi perfil, lista de casos con filtro, formulario Nuevo caso, pestañas Datos, Escenarios y preguntas, Rúbrica y Publicación de un caso publicado, Validar caso, Vista previa, panel Importar caso, Mis sesiones, calendario, formulario Programar sesión (sin enviar), ficha de la sesión (recorrido, condiciones, convocados, Ver intentos, avisos por correo), ventana Autorizar repetición, Participaciones, Resultados y reportes, Estadísticas, Mis casos del estudiante, ¿Puedo ingresar? y Mis resultados.

## Funciones verificadas solo en el código

Crear y editar casos, escenarios, preguntas, opciones y rúbricas; publicar, archivar, duplicar, exportar, importar y eliminar casos; programar y modificar sesiones; agregar y retirar estudiantes; enviar convocatoria y notificar cambios; iniciar y finalizar sesiones; cierres automáticos; desarrollo del caso por el estudiante (página de participación), guardado de respuestas, reloj, finalización; cálculo de la nota; nota oficial; retroalimentación; Cerrar intentos pendientes; Publicar calificaciones; autorización de repetición; descarga de reportes PDF, Excel y CSV.

## Capturas que no fue posible obtener

- Página de participación del estudiante (desarrollo del caso, mapa de preguntas, reloj, ventana Antes de finalizar): exige iniciar un intento y la sesión de prueba estaba Sin iniciar.
- Resultados con datos (tabla, gráficas, detalle, nota oficial, retroalimentación) para docente y estudiante: la base de prueba no tiene intentos.
- Sesión En curso o Finalizada, y caso en Borrador con botones de edición: no había datos en ese estado y no se autorizó crearlos.
- Mensajes de credenciales inválidas y bloqueo: no se probaron para no activar el bloqueo del correo.

## Pendientes, limitaciones y diferencias con lo esperado

- No hay grupos ni cursos como registros aparte: el grupo es el texto «Espacio académico» de la sesión.
- No hay pantalla para crear o inscribir estudiantes. El administrador solo crea docentes; en la base de prueba los estudiantes vienen de las semillas.
- No existe un apartado de personajes o diálogos en los casos: esa información va dentro de los textos del caso y de los escenarios.
- No hay recuperación de contraseña, registro abierto ni ingreso con Google, Microsoft o Apple: los botones solo muestran avisos.
- La rúbrica define la escala de la nota (su mayor rango máximo), pero no se califica criterio por criterio; todas las preguntas pesan 1 (supuesto también anotado en el README del proyecto).
- **Diferencia 1:** la página del caso, al terminar, dice «Puedes ver tu resultado cuando el docente lo publique», pero el backend calcula la nota al finalizar y la muestra de inmediato en Mis resultados.
- **Diferencia 2:** el aviso de «Publicar calificaciones» dice que se envía la nota oficial, pero el backend envía un correo por cada intento con su nota calculada.
- El estudiante no tiene un botón para pedir el segundo intento: debe hablar con el docente.
- La pestaña Herramientas del caso y la sección Herramientas de apoyo de la participación corresponden a RF-28, que no hace parte del PMV, y no se documentan.
