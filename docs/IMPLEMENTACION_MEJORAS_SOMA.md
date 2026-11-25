# Implementación de las mejoras de administración e interfaz de SOMA

Fecha de la implementación: 25 de noviembre de 2026. Cierre de la auditoría: el mismo día, en una segunda pasada.
Repositorio: `C:\Users\Sofia\Desktop\SOMA\Segunda-entrega-proyecto-nuclear`.
Remoto: `https://github.com/Samue1roj45/Segunda-entrega-proyecto-nuclear`.
Rama: `feature/mejoras-gestion-usuarios-auditoria-casos`, creada desde el commit `09317be`.
No se creó otro clon ni otra carpeta del proyecto. No se hizo commit ni push.

La rama ya existía y contenía el desarrollo sin confirmar. Se conservaron esos cambios. En esta pasada se añadieron la prueba de concurrencia real, la prueba de que la limpieza de hoy no borra su propio registro, y el texto en singular cuando se elimina un solo registro de auditoría.

## 1. Objetivo

Completar y verificar, sobre el código real de SOMA, la administración segura de cuentas, la limpieza del historial de auditoría, la uniformidad de botones, el ciclo de archivado y recuperación de casos, y la presentación de los estudiantes convocados.

## 2. Alcance

| Ítem | Qué quedó implementado |
|---|---|
| Usuarios | Listar activos y archivados, filtrar por rol, buscar por nombre o correo, archivar, reactivar y eliminar solo si no hay historial protegido. |
| Auditoría | Vista previa por fecha de corte, confirmación explícita, borrado transaccional y registro `AUDITORIA_LIMPIADA` de la propia operación. |
| Botones | Alturas, tipografía y estados compartidos. La clase inexistente `boton-secundario` ya no se usa. |
| Casos | Filtros Todos, Activos, Borradores y Archivados. Archivar solo un publicado. Recuperar con revalidación. Duplicar mediante el DSL existente. |
| Convocados | Nombre en la primera línea y correo debajo. Los botones de la fila no cambiaron de comportamiento. |

Quedan fuera de alcance, y sin cambio de lógica, las convocatorias, reconvocatorias, códigos, cupos, horarios, espacios, retiros individual y masivo, motivos y validaciones de retiro, y los estados de participación de esos flujos.

## 3. Arquitectura

Se mantiene el monolito modular en Java 21 y Spring Boot, con Angular y MySQL. No se añadieron dependencias ni se cambió el esquema: no hay migración nueva. Las operaciones usan columnas que ya existían (`usuario.activo`, `casos_estudio.estado`, `auditoria.fecha`).

La base de desarrollo publicada en el puerto 3307 no se usó para estas pruebas manuales. La verificación en el navegador se hizo contra la pila Docker `soma_verif_*` (interfaz en `http://localhost:4201`, API en `8081`), con usuarios ficticios de la semilla.

## 4. Administración de usuarios

Pantalla `/usuarios`. El servidor expone:

- `GET /api/admin/usuarios?activos=&rol=`
- `POST /api/admin/usuarios/{id}/archivo`
- `POST /api/admin/usuarios/{id}/reactivacion`
- `GET /api/admin/usuarios/{id}/eliminacion`
- `DELETE /api/admin/usuarios/{id}`

La búsqueda por nombre o correo se aplica en la pantalla sobre la lista que ya devolvió el servidor. El filtro de rol sí viaja al backend.

### Reglas de seguridad

Todas se aplican en el servidor, dentro de la transacción, con bloqueo pesimista. Ocultar botones no es la protección.

- Nadie archiva ni elimina su propia cuenta.
- Nunca puede quedar cero administradores activos. Dos archivos simultáneos de administradores distintos se serializan: el primero bloquea las filas de los administradores activos y el segundo, al continuar, ve que ya no queda otro administrador y recibe 409.
- Un administrador sí puede archivar a otro administrador si queda al menos uno activo.
- La eliminación definitiva se rechaza si hay casos (incluida la papelera), sesiones, convocatorias, repeticiones, participaciones, notas, retroalimentaciones o notificaciones. La respuesta nombra cada grupo y su cantidad. La interfaz ofrece archivar en su lugar.
- Permisos, avisos y preferencias se borran con la cuenta porque la base los declara en cascada. La auditoría no se borra: guarda el correo del actor como texto.
- También se cuentan las tablas sin llave foránea hacia `usuario`: participación, nota oficial, retroalimentación y notificación.
- Un docente o un estudiante que llame directo a estos endpoints recibe 401 o 403.
- Si aparece un registro asociado entre la validación y el borrado, la violación de integridad responde 409.

## 5. Limpieza de auditoría

`GET /api/auditoria/limpieza?antesDe=` solo cuenta. `POST /api/auditoria/limpieza` exige `confirmada: true`.

La fecha es obligatoria y no puede ser posterior a hoy. El corte es el inicio de ese día: se eliminan los registros **anteriores** a ese día y se conservan los de ese día en adelante. Es lo que dice la pantalla («Elimina los registros anteriores a la fecha que elijas. Los registros de esa fecha en adelante se conservan»).

El borrado y el alta de `AUDITORIA_LIMPIADA` van en la misma transacción. El registro nuevo se inserta después del `DELETE` y lleva la fecha actual, así que esa misma ejecución no puede borrarlo. El detalle incluye al administrador, la fecha de corte, la cantidad y el resultado (`correcto`). Si la operación falla, el borrado se revierte y queda un registro `AUDITORIA_LIMPIEZA_FALLIDA`.

La paginación (`limite`, `desplazamiento`) y el filtro por acción del listado se mantienen.

## 6. Casos archivados

- Solo un caso `PUBLICADO` puede archivarse. El contenido y las sesiones anteriores siguen ligados al mismo caso.
- Recuperar comprueba que esté `ARCHIVADO`, vuelve a validar las reglas de publicación y, si cumple, lo deja en `PUBLICADO`. Si no cumple, responde el motivo y el caso sigue archivado.
- Duplicar no es una operación nueva. La lista exporta el DSL del caso y lo vuelve a importar con el nombre terminado en «(copia)». La copia nace en borrador y no modifica el original ni sus sesiones.
- El DSL copia nombre, desarrollo, contexto, tiempo estimado, escenarios, preguntas, respuestas (incluida la marca de correcta, el porcentaje y la justificación) y la rúbrica con sus criterios cuando el caso la tiene.
- El DSL no copia herramientas profesionales: `DocumentoDsl` exporta esa lista vacía. Tampoco copia sesiones, convocatorias ni historial de uso.

## 7. Convocados y botones

En `sesion-detalle.ts` el diff es solo de marcado: la fila del convocado usa `.quien` (nombre arriba, correo debajo) y `.fila-persona`. Los botones «Ver intentos», «Autorizar repetición» y «Retirar», y sus manejadores, quedan igual.

`styles.css` define `.boton-primario`, `.boton-suave`, `.boton-peligro` y `.boton-chico` con la misma altura base, radio y estados `disabled` y `aria-busy`. En pantallas estrechas el texto del botón puede partirse. No queda ninguna referencia a `boton-secundario` en `frontend/soma-web`.

## 8. Archivos principales

Nuevos de administración y limpieza: `ServicioAdministracionUsuarios`, `ServicioAdministracionUsuariosImpl`, `CadenasAdministracionCuentas`, `ValidadorCuentaPropia`, `ValidadorAdministradorRestante`, `ValidadorSinRegistrosHistoricos`, `RegistrosHistoricosUsuario`, `RegistrosHistoricosUsuarioJdbc`, `ObservadorCuentasAdministradas`, `AdministracionUsuariosControlador`, `LimpiezaAuditoria`, y la página `frontend/soma-web/src/app/paginas/admin/usuarios.ts`.

Modificados para el ciclo de casos: `EstadoCaso`, `CasoEstudio`, `FachadaPublicacionCasos`, `PublicacionCasos`, `PublicacionControlador`, `casos-lista.ts` y `caso-editor.ts`.

## 9. Pruebas ejecutadas en esta sesión

| Suite | Resultado |
|---|---|
| `mvn -q test` en `backend`, sesión anterior, H2 | Código 0. Surefire: 285 pruebas, 0 fallos, 0 errores y 21 omitidas. Esas 21 no se ejecutaron en esa corrida. |
| `LimpiezaAuditoriaIntegracionTest` | Reejecutada en el cierre, H2. 3 pruebas, 0 fallos, 0 omitidas. |
| `CasosApiIntegracionTest#recuperacion` | Reejecutada en el cierre, H2. 1 prueba, 0 fallos. |
| `CasosApiMysqlTest` | 20 pruebas, 0 fallos, 0 omitidas. MySQL 8.0.45, base vacía `plataforma_pruebas`. |
| `FlujoCompletoMysqlTest` | 1 prueba (`flujoCompletoEntreModulos`), 0 fallos, 0 omitidas. MySQL 8.0.45, base vacía `plataforma_flujo`, con `ddl-auto=validate`. |
| Concurrencia del último administrador | 1 prueba, 0 fallos, sobre MySQL 8.0.45 en `plataforma_concurrencia`. El detalle está en la sección 15. |
| `ng test --watch=false` y `ng build` | Sesión anterior: 24 pruebas de Angular, 0 fallos, y la compilación terminó bien. No se repitieron en el cierre. |
| Playwright (`npm run e2e`) | En la auditoría previa, 8 pruebas pasaron usando Chrome del sistema mediante `SOMA_CHROMIUM`. No se repitió en el cierre. Esas pruebas usan el servidor simulado; no recorren usuarios, auditoría ni casos archivados. |

### Cómo se ejecutaron las pruebas MySQL

Se levantó un contenedor nuevo, `soma_cierre_mysql`, con MySQL 8.0 publicado solo en `127.0.0.1:3308`. No se usó la base de desarrollo del puerto 3307 ni la pila `soma_verif_*`. Dentro de ese contenedor se crearon cuatro bases vacías: `plataforma_pruebas`, `plataforma_flujo`, `plataforma_concurrencia` y `plataforma_cierre`. Flyway aplicó migraciones y semilla al arrancar cada contexto. La clave quedó solo en el contenedor; no se escribió en el repositorio.

`CasosApiMysqlTest` se activa con `MYSQL_PRUEBAS_URL` no vacío. Usa además `MYSQL_PRUEBAS_USUARIO` y `MYSQL_PRUEBAS_CLAVE`.

`FlujoCompletoMysqlTest` se activa con `MYSQL_FLUJO_URL`. Usa las mismas `MYSQL_PRUEBAS_USUARIO` y `MYSQL_PRUEBAS_CLAVE`, y fija `spring.jpa.hibernate.ddl-auto=validate`.

Comando, desde `backend`, con las dos URL apuntando a bases distintas del contenedor desechable:

```text
mvn "-Dtest=CasosApiMysqlTest,FlujoCompletoMysqlTest" test
```

Resultado de Surefire: 21 pruebas, 0 fallos, 0 errores, 0 omitidas. Hibernate registró `MySQLDialect` y las URL `plataforma_pruebas` y `plataforma_flujo`.

La prueba de concurrencia crea un segundo administrador con la misma clave de prueba, lanza dos hilos que se archivan entre sí y comprueba que queda exactamente un administrador activo, con respuestas 200 y 409. El 409 contiene «último administrador». Al final reactiva ambas cuentas. Esa aserción no demuestra que las dos transacciones se hayan solapado: el mismo resultado aparece si una termina antes de que la otra entre. No se añadieron esperas artificiales.

## 10. Verificación en el navegador

La primera pasada se hizo contra `http://localhost:4201`. El cierre se hizo contra el código actual en `http://localhost:4202`, con el backend en el puerto 8082 y la base desechable `plataforma_cierre`. Usuarios ficticios de la semilla. No se sobrescribió `docs/evidencias-mejoras-A-E/`.

| Evidencia | Archivo |
|---|---|
| Usuarios activos | `docs/evidencias-implementacion-soma/01-usuarios-activos.png` |
| Usuarios archivados (María, cuenta ficticia) | `docs/evidencias-implementacion-soma/02-usuarios-archivados.png` |
| Confirmación de archivado | `docs/evidencias-implementacion-soma/03-confirmacion-archivado-usuario.png` |
| Eliminación bloqueada por convocatorias | `docs/evidencias-implementacion-soma/04-bloqueo-eliminacion-por-historial.png` |
| Vista previa de limpieza (1 registro anterior al 25 de noviembre de 2026) | `docs/evidencias-implementacion-soma/05-vista-previa-limpieza-auditoria.png` |
| Confirmación de limpieza | `docs/evidencias-implementacion-soma/06a-confirmacion-limpieza-auditoria.png` |
| Resultado de la limpieza en el cierre: «1 registro», el ingreso del día se conserva | `docs/evidencias-implementacion-soma/06b-resultado-limpieza-registro-propio.png` |
| Caso archivado, con Recuperar y Duplicar | `docs/evidencias-implementacion-soma/07-casos-archivados.png` |
| Confirmación de recuperar | `docs/evidencias-implementacion-soma/08a-confirmacion-recuperar-caso.png` |
| Botones de la lista y borrador «Copia editada en la verificación» | `docs/evidencias-implementacion-soma/10-botones-lista-casos.png` |
| Convocados con nombre y correo; retiros visibles | `docs/evidencias-implementacion-soma/11-convocados-nombre-y-correo.png` |
| La sesión de prueba sigue en «Mis sesiones», con Abrir e Iniciar | `docs/evidencias-implementacion-soma/12-sesiones-sin-cambios.png` |
| Caso archivado en el cierre, con Recuperar y Duplicar | `docs/evidencias-implementacion-soma/07b-caso-archivado-antes-de-duplicar.png` |
| Caso recuperado, de nuevo en Activos | `docs/evidencias-implementacion-soma/08b-caso-recuperado-en-activos.png` |
| Duplicado en borrador, con nombre «(copia)» | `docs/evidencias-implementacion-soma/09-caso-duplicado-borrador.png` |
| Confirmación de eliminar una cuenta sin historial | `docs/evidencias-implementacion-soma/04b-confirmacion-eliminacion-sin-historial.png` |
| Lista de usuarios después de esa eliminación | `docs/evidencias-implementacion-soma/04c-usuario-sin-historial-eliminado.png` |
| Convocados en vista de 390 px de ancho | `docs/evidencias-implementacion-soma/11b-convocados-vista-estrecha.png` |

La carpeta `docs/evidencias-mejoras-A-E/` no se modificó. `06b` se sustituyó por la captura nueva porque la anterior decía «1 registros».

## 11. Problemas y ajustes

- La prueba nueva de limpieza de «hoy» compartía la base H2 con la prueba del corte del 1 de noviembre y hacía fallar un conteo que incluía registros `AUDITORIA_LIMPIADA` previos. El conteo ahora compara el mismo conjunto, sin borrar la prueba.
- El mensaje de limpieza decía «1 registros». Quedó «1 registro» cuando la cantidad es uno, y «N registros» en los demás casos.
- La primera corrida de Playwright falló por falta de `headless_shell.exe`. Después, con `SOMA_CHROMIUM` apuntando a Chrome instalado, las 8 pruebas del recorrido simulado pasaron. El cierre no las repitió.

## 12. Limitaciones

- Duplicar no copia herramientas profesionales: `DocumentoDsl.de` exporta esa lista vacía. Tampoco copia sesiones, convocatorias ni historial de uso. En la base desechable el caso de semilla no tenía filas en `herramientas_caso`, y la copia tampoco.
- La búsqueda de usuarios filtra en el navegador la página ya cargada; el servidor filtra por estado y por rol.
- La prueba de concurrencia comprueba el resultado final, no el solapamiento de las dos transacciones. El bloqueo de MySQL se comprobó aparte, con dos sesiones, como describe la sección 15.
- El aviso flotante de la pantalla sigue armando la frase «se eliminaron N registros» también cuando N es 1. El texto que queda guardado en la auditoría sí usa «1 registro».
- La semilla solo da a Carlos Gómez el permiso `GESTIONAR_CASOS`. Para abrir la sesión en la base desechable se le añadió `PROGRAMAR_SESIONES` ahí, no en el código ni en la base del puerto 3307. No se retiró a nadie ni se inició la sesión.

## 13. Patrones y SOLID

| Modificación | Patrón utilizado | Clases involucradas | Justificación | Principios SOLID |
|---|---|---|---|---|
| Autorización de la administración de usuarios | Proxy, reutilizado | `ProxyAcceso`, `ServicioAdministracionUsuarios`, `ConfiguracionAdministracionDocentes` | El permiso se declara en la interfaz y el proxy falla cerrado, igual que el resto de servicios | SRP, OCP |
| Reglas de archivar y eliminar | Chain of Responsibility, reutilizada | `ValidadorBase`, `CadenasAdministracionCuentas`, `ValidadorCuentaPropia`, `ValidadorAdministradorRestante`, `ValidadorSinRegistrosHistoricos` | Cada regla es un eslabón. Archivar y eliminar arman cadenas distintas sin copiar los `if` | SRP, OCP, LSP |
| Auditoría y avisos después del cambio de cuenta | Observer, mismo mecanismo que las sesiones | `CuentaAdministradaEvento`, `ObservadorCuentasAdministradas`, `ServicioAvisos` | Escribir avisos dentro de la transacción que tiene bloqueada `usuario` podía esperar su propio bloqueo. El observador actúa después del commit | SRP, DIP |
| Historial que impide borrar | Ninguno. Puerto y adaptador por DIP | `RegistrosHistoricosUsuario`, `RegistrosHistoricosUsuarioJdbc` | El módulo de usuarios no conoce las tablas académicas | DIP, ISP |
| Limpieza de auditoría | Ninguno | `LimpiezaAuditoria`, `ServicioAvisos`, `AvisoControlador` | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | SRP |
| Botones | Ninguno | `styles.css` y las plantillas | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | — |
| Recuperar un caso | Facade y Proxy, reutilizados | `FachadaPublicacionCasos`, `PublicacionCasos`, `CasoEstudio`, `EstadoCaso` | Recuperar entra por la misma fachada que publicar y archivar, y el proxy exige `GESTIONAR_CASOS` | SRP, OCP |
| Transición ARCHIVADO a PUBLICADO | Ninguno. El ciclo sigue en el enum `EstadoCaso`; no es el patrón State | `EstadoCaso`, `CasoEstudio` | Una jerarquía de estados no aportaba: las transiciones ya viven en el enum y en la entidad | OCP |
| Duplicar | Builder, reutilizado y sin cambios | `DocumentoDsl`, `ImportadorDsl`, `CasoEstudioBuilder` | La copia recorre el importador que ya construye el caso | SRP |
| Nombre y correo de los convocados | Ninguno | `sesion-detalle.ts`, `styles.css` | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | — |

No se añadió el patrón State: el archivo de una cuenta es el booleano `activo` de `Usuario`, y el de un caso sigue siendo el enum `EstadoCaso`.

## 14. Procesos que permanecen intactos

`git diff` no muestra cambios en los paquetes de sesiones ni de evaluación del backend, ni en las migraciones. En `sesion-detalle.ts` el diff son 6 líneas añadidas y 4 quitadas, todas del marcado de la fila del convocado. En la sesión abierta durante el cierre se seguían viendo «Ver intentos», «Autorizar repetición» y «Retirar», también en la vista de 390 px. No se usó ninguno de esos botones.

## 15. Cierre de la auditoría

Hecho el 25 de noviembre de 2026 sobre la rama `feature/mejoras-gestion-usuarios-auditoria-casos`, sin commit ni push.

### Limpieza de auditoría

`LimpiezaAuditoriaIntegracionTest` pasó las 3 pruebas en H2. Comprueban que se borra solo lo anterior al inicio del día de corte, que se conservan el día de corte y los posteriores, que `AUDITORIA_LIMPIADA` se inserta después del `DELETE` y sobrevive a una segunda limpieza con el mismo corte, y que el listado sigue filtrando por acción y paginando con `limite=1`.

En la aplicación, con un único registro del 1 de noviembre y el ingreso del 25 de noviembre, la vista previa dijo «1 registro anterior». Después de confirmar, la base quedó así: el registro del 1 de noviembre desapareció; el ingreso del 25 de noviembre siguió; apareció `AUDITORIA_LIMPIADA` del mismo día con el texto «se eliminaron 1 registro anteriores al 25 de noviembre de 2026. Resultado: correcto». El filtro «Historial limpiado» dejó solo esa fila. La paginación no se volvió a pulsar en pantalla porque había dos filas; la cubre la prueba de integración.

### Recuperación y duplicación

`CasosApiIntegracionTest.recuperacion` pasó en H2: un borrador y un publicado responden 409 al recuperar; un archivado vuelve a `PUBLICADO`, sale de la lista de archivados, conserva su escenario y no se puede recuperar otra vez.

En la aplicación se archivó «Conformidad en el grupo de trabajo». Apareció en Archivados, con Recuperar y Duplicar. Duplicar creó el caso 2, en `BORRADOR`, con el nombre terminado en «(copia)» y el editor en `/casos/2`. El caso 1 siguió `ARCHIVADO` hasta que se recuperó. Después de recuperar, el caso 1 volvió a `PUBLICADO`, con `fecha_archivado` nula, y el caso 2 siguió en borrador. El desarrollo, el contexto y el tiempo de ambos producen el mismo resumen; cada uno tiene 1 escenario, 2 preguntas, 4 respuestas y 1 rúbrica. La sesión de semilla sigue ligada solo al caso 1, en `SIN_INICIAR`. `herramientas_caso` tiene 0 filas.

### Concurrencia y bloqueo

En H2, la misma prueba ya había pasado en la auditoría anterior. En este cierre se ejecutó otra vez contra MySQL 8.0.45, base vacía `plataforma_concurrencia`, sobrescribiendo el datasource del perfil de prueba con `SPRING_DATASOURCE_URL`. Resultado: 1 prueba, 0 fallos, 0 omitidas, dialecto `MySQLDialect`. El mecanismo de producción no se cambió.

El código pide `LockModeType.PESSIMISTIC_WRITE` en `UsuarioRepositorio.bloquearAdministradoresActivos` y en `buscarParaModificar`. Para ver el bloqueo de MySQL, una sesión hizo `SELECT ... FOR UPDATE` sobre los administradores activos y esperó 15 segundos. La otra, con `innodb_lock_wait_timeout=3`, recibió `ERROR 1205 (HY000): Lock wait timeout exceeded`. Eso comprueba que MySQL serializa ese bloqueo. No comprueba, por sí solo, que las dos peticiones HTTP de la prueba se hayan encontrado dentro de la misma ventana.

### Lo que sigue fuera de esta comprobación

No se volvió a ejecutar la suite completa de 285 pruebas. No se repitió Playwright. No se modificó la base del puerto 3307 ni los volúmenes de `soma_verif_*`. El contenedor `soma_cierre_mysql` quedó en marcha, en el puerto 3308, con datos ficticios.
