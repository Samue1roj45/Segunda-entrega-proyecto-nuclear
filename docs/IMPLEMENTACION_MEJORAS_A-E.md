# Implementación y verificación de las mejoras A–E de SOMA

Rama: `feature/mejoras-gestion-usuarios-auditoria-casos` (creada desde `09317be`). Los cambios están sin confirmar: no se hizo commit ni push.
Fecha de verificación: 25 de noviembre de 2026.

## 1. Objetivo y alcance

Este documento integra y verifica cinco mejoras administrativas y de interfaz:

| Ítem | Mejora |
|---|---|
| A | Administración de usuarios (docentes y estudiantes): listar activos y archivados, archivar, reactivar y eliminar de forma segura. |
| B | Limpieza del historial de auditoría hasta una fecha de corte, con vista previa, confirmación explícita, operación transaccional y registro de la propia limpieza. |
| C | Sistema de botones uniforme. Se corrige la clase inexistente `boton-secundario`. |
| D | Ciclo de vida de los casos: archivar desde la lista (solo publicados), filtros Todos/Activos/Borradores/Archivados, recuperar archivados (con revalidación) y reutilizar mediante Duplicar. |
| E | Lista de convocados de una sesión con el nombre arriba y el correo debajo. |

Quedan **fuera de alcance y sin modificar** los apartados F y G (convocatorias, reconvocatorias, códigos, cupos, retiros y sus motivos y validaciones, y los estados de participación de esos flujos).

## 2. Decisiones funcionales acordadas

- La pantalla de usuarios cubre docentes y estudiantes.
- Cuentas protegidas: nadie puede archivar ni eliminar su propia cuenta, y nunca puede quedar cero administradores activos. Un administrador sí puede gestionar otras cuentas de administrador.
- La eliminación definitiva se bloquea si la cuenta tiene registros académicos o de comunicación: casos (incluida la papelera), sesiones, convocatorias, repeticiones, participaciones, notas, retroalimentaciones y notificaciones. Los permisos, avisos y preferencias sí se eliminan con la cuenta. La auditoría se conserva porque guarda el correo del actor.
- Se revisan también las tablas que no tienen llave foránea hacia `usuario`.

## 3. Arquitectura y componentes afectados

Se conserva la arquitectura existente: Spring Boot 4.1 con Java 21 y Angular 21 standalone con signals. No se cambiaron versiones de dependencias ni se añadieron tecnologías.

### 3.1 Backend (`backend/src/main/java/co/edu/humboldt/plataformacasos`)

**A — Administración de usuarios. Archivos nuevos:**

- `usuarios/administracion/ServicioAdministracionUsuarios`: interfaz anotada con `@RequierePermiso(ADMINISTRAR_DOCENTES)`.
- `usuarios/administracion/ServicioAdministracionUsuariosImpl`: implementación. Bloquea a los administradores activos y luego a la cuenta objetivo, siempre en ese orden.
- `usuarios/administracion/CadenasAdministracionCuentas`: arma las cadenas de archivo y de eliminación.
- Eslabones de validación: `ValidadorCuentaPropia`, `ValidadorAdministradorRestante` y `ValidadorSinRegistrosHistoricos`.
- `RegistrosHistoricosUsuario`: puerto. Su adaptador es `integracion/RegistrosHistoricosUsuarioJdbc`.
- `CuentaAdministradaEvento` y `ObservadorCuentasAdministradas`: auditoría y avisos después del commit.
- DTO: `SolicitudCambioCuenta`, `UsuarioAdminDTO`, `RegistroHistorico` y `EvaluacionEliminacionDTO`.
- `usuarios/web/AdministracionUsuariosControlador`, con estos endpoints:
  - `GET /api/admin/usuarios?activos=&rol=`
  - `POST /{id}/archivo`
  - `POST /{id}/reactivacion`
  - `GET /{id}/eliminacion` (evaluación previa)
  - `DELETE /{id}`

**A — Archivos modificados:**

- `Usuario`: métodos `archivar()` y `reactivar()`.
- `UsuarioRepositorio`: consultas `listarPorEstado`, `buscarParaModificar` y `bloquearAdministradoresActivos`, estas dos últimas con `PESSIMISTIC_WRITE`.
- `MensajesAdministracion`: mensajes nuevos.
- `ConfiguracionAdministracionDocentes`: bean `@Primary` protegido con `ProxyAcceso`.

**B — Limpieza de auditoría:**

- Nuevo `avisos/LimpiezaAuditoria`, con dos operaciones:
  - Vista previa de solo lectura.
  - Ejecución en `TransactionTemplate`: borra y registra la limpieza en la misma transacción. Si falla, deja un registro de fallo.
- `ServicioAvisos`: método `registrarEnLaMismaTransaccion` (propagación `MANDATORY`).
- `AvisoControlador`: `GET /api/auditoria/limpieza?antesDe=` y `POST /api/auditoria/limpieza`. El POST exige `confirmada: true`.

**D — Ciclo de vida de casos:**

- `EstadoCaso`: transición ARCHIVADO → PUBLICADO y métodos `puedePublicarse()` y `puedeRecuperarse()`.
- `CasoEstudio`: métodos `exigirPublicable()` y `recuperar()`.
- `PublicacionCasos`: método `recuperar` con `@RequierePermiso(GESTIONAR_CASOS)`.
- `FachadaPublicacionCasos`: método `recuperar` con revalidación, y auditoría de archivado y recuperación.
- `PublicacionControlador`: `POST /api/casos/{id}/recuperacion`.

### 3.2 Frontend (`frontend/soma-web/src/app`)

- **A:** nueva página `paginas/admin/usuarios.ts` en la ruta `/usuarios`. También se modificaron:
  - `app.routes.ts`
  - `panel.ts` (entrada de navegación «Usuarios»)
  - `inicio.ts` (acceso directo)
  - `docentes.ts` (solo muestra docentes activos y enlaza a `/usuarios`)
- **B:** `auditoria.ts`, que gana la sección «Limpiar historial de auditoría», nombres de acciones nuevos y un listado corregido (ver 8.2).
- **C:** `styles.css`, con el sistema de botones unificado:
  - `boton-primario`, `boton-suave` y `boton-peligro` comparten altura, radio y tipografía; `boton-chico` es la variante pequeña.
  - Estados `disabled` y `aria-busy`.
  - Comportamiento responsivo.
  - `boton-secundario` se reemplazó en todos sus usos.
- **D:** `casos-lista.ts` (filtros, Archivar, Recuperar, Duplicar y estado inmediato en la tarjeta) y `caso-editor.ts`.
- **E:** `sesion-detalle.ts`, donde solo cambió el marcado de la fila de convocado, más los estilos `.quien` y `.fila-persona`. Los botones y sus condiciones quedaron intactos.
- **Compartido:**
  - `soma-api.service.ts`: métodos nuevos.
  - `dominio.ts`: tipos nuevos.
  - `servicios.spec.ts`: tres pruebas.

## 4. Reglas de seguridad aplicadas

- Todos los endpoints nuevos exigen `ADMINISTRAR_DOCENTES`, salvo la recuperación de casos, que exige `GESTIONAR_CASOS`. `ProxyAcceso` falla cerrado si un método no está anotado.
- Las cuentas protegidas se validan en el servidor con una cadena de responsabilidad. La interfaz también lo refleja: la fila propia muestra «Tu cuenta» y no tiene acciones.
- **Concurrencia:** hay bloqueo pesimista con un orden consistente (primero los administradores activos, luego la cuenta). Si alguien crea un registro académico entre la evaluación y el borrado, la violación de integridad se responde con 409.
- La auditoría y los avisos se escriben después del commit (observador). Así se evitan esperas de bloqueo sobre `usuario`.
- **Limpieza de auditoría:**
  - La fecha es obligatoria y no puede ser futura.
  - Requiere confirmación explícita.
  - Es atómica: el borrado y su propio registro van juntos o no ocurren.

## 5. Base de datos

**No hay cambios de esquema ni migraciones nuevas.** Las migraciones existentes (V1–V45) no se tocaron. Las funciones nuevas usan columnas que ya existían: `usuario.activo`, `casos_estudio.estado` y `auditoria.fecha`. La base de desarrollo del usuario (`soma_db`) no se modificó.

Toda la verificación funcional se hizo en una pila desechable aparte, el proyecto Docker `soma-verificacion` con puertos 4201, 8081 y 8026. Se cargó con datos ficticios marcados como prueba (`datos-prueba-verificacion.sql`).

## 6. Pruebas automatizadas

| Suite | Resultado |
|---|---|
| Backend (`mvn test`, H2) | 302 pruebas, 0 fallos, 0 errores, 21 omitidas. Las omitidas son `CasosApiMysqlTest` y `FlujoCompletoMysqlTest`: requieren `MYSQL_FLUJO_URL` y ya se omitían antes. |
| Frontend (`ng test`, vitest) | 24/24 |
| Frontend (`ng build`) | Compila sin errores |
| e2e Playwright | 8/8 |
| Verificación funcional sobre MySQL real (`verificar-y-capturar.cjs`) | 31/31 pasos, en dos rondas |

Pruebas nuevas o actualizadas:

- `AdministracionUsuariosIntegracionTest`: archivar y reactivar, eliminación segura, historial sin llave foránea, cuentas protegidas y permisos.
- `CadenasAdministracionCuentasTest`: incluye el caso del último administrador.
- `LimpiezaAuditoriaIntegracionTest`.
- `UsuarioTest`.
- `FachadaPublicacionCasosTest`: recuperación y recuperación de un caso no archivado.
- `CasosApiIntegracionTest`: recuperación y recuperación sin permiso (401/403).
- `CasoEstudioTest.cicloDeVida`: **se actualizó, no se eliminó**. Antes prohibía desarchivar; ahora comprueba la nueva regla de recuperación.

## 7. Evidencias (`docs/evidencias-mejoras-A-E/`)

Son capturas reales de la aplicación ejecutándose contra MySQL, tomadas con datos ficticios.

| # | Evidencia requerida | Archivo(s) |
|---|---|---|
| 1 | Usuarios activos | `01-usuarios-activos.png` |
| 2 | Usuarios archivados | `02-usuarios-archivados.png` |
| 3 | Confirmación de archivado | `03-confirmacion-archivado-usuario.png` |
| 4 | Eliminación bloqueada por historial | `04-bloqueo-eliminacion-por-historial.png` (complemento: `04b-confirmacion-eliminacion-sin-historial.png`) |
| 5 | Vista previa de la limpieza de auditoría | `05-vista-previa-limpieza-auditoria.png` |
| 6 | Limpieza y registro de la propia limpieza | `06a-confirmacion-limpieza-auditoria.png`, `06b-resultado-limpieza-registro-propio.png` |
| 7 | Casos archivados | `07-casos-archivados.png` |
| 8 | Recuperar caso | `08a-confirmacion-recuperar-caso.png`, `08b-caso-recuperado-en-activos.png` |
| 9 | Reutilizar caso (duplicado como borrador) | `09-caso-duplicado-como-borrador.png` |
| 10 | Botones uniformes | `10-botones-lista-casos.png`, `10b-papelera-restaurar.png` |
| 11 | Convocados con nombre y correo | `11-convocados-nombre-y-correo.png`, `11b-convocados-pantalla-pequena.png` |
| 12 | Funcionalidades F/G sin cambios | `12-sesiones-sin-cambios.png` |

## 8. Problemas encontrados y soluciones

1. **Riesgo de espera de bloqueo.** Insertar avisos con llave foránea en una transacción `REQUIRES_NEW` mientras la transacción externa tiene `FOR UPDATE` sobre `usuario` podía bloquearse a sí mismo. Se resolvió así:
   - Con un observador que actúa después del commit.
   - Con un orden de bloqueo consistente.
2. **Listado de auditoría (defecto previo, detectado en la captura 06b).** El detalle aparecía pegado al correo y con viñetas. La causa es que `preserveWhitespaces:false` elimina el espacio entre elementos en línea. Se cambió el marcado a `lista-simple` con `.quien` (detalle arriba, correo y fecha debajo) y se repitió la verificación completa con un volumen nuevo: 31/31. Las capturas de la primera ronda se guardaron aparte y no se sobrescribieron.
3. **Mismo defecto de espacios en la lista de convocados (E).** Se resolvió con el bloque `.quien` en rejilla.
4. **Diferencias de fin de línea (CRLF).** Generaban ruido en `git status`. El estado se revisa con `core.autocrlf=true`, y los archivos se escribieron en CRLF para respetar el formato del repositorio.

## 9. Limitaciones y pendientes conocidos

- Las pruebas MySQL del backend se omiten si no está definida `MYSQL_FLUJO_URL`. Esto ya ocurría antes de los cambios.
- La regla del último administrador solo puede activarse por concurrencia, porque la regla de cuenta propia actúa antes. Está cubierta por la prueba unitaria de la cadena.
- Duplicar no copia las herramientas del caso. Es el comportamiento previo y no se modificó.
- El botón «Eliminar» de usuarios siempre se muestra. La posibilidad de eliminar se evalúa al hacer clic y, si está bloqueada, se explica el motivo y se ofrece archivar.
- La pila de verificación (`soma-verificacion`) y sus volúmenes `soma_verificacion_mysql` y `soma_verificacion_mysql_2` siguen en el equipo para que el usuario los elimine cuando quiera. No contienen datos reales.
- Se generaron `node_modules`, `dist` y `target`, que están ignorados por git.

## 10. Confirmación sobre F y G

`git diff` no muestra cambios en:

- Los paquetes de sesiones y evaluación del backend.
- `ObservadorSesiones` y `CatalogoAcademicoIntegrado`.
- Las migraciones.

En `sesion-detalle.ts` solo cambió el marcado de la fila de convocado (requisito E). Los botones «Ver intentos», «Autorizar repetición» y los de retiro conservan exactamente sus condiciones y manejadores. No se modificaron convocatorias, reconvocatorias, generación de códigos, cupos, retiros, motivos ni validaciones de retiro, ni los estados de participación de esos flujos.

## 11. Patrones de diseño y principios SOLID

Solo se usan los patrones permitidos, y únicamente cuando se justifican.

### A — Administración de usuarios

**Proxy (reutilizado)**
- *Problema:* las operaciones administrativas deben exigir permisos sin mezclar autorización y lógica.
- *Razón:* `ProxyAcceso` ya protege todos los servicios del sistema y falla cerrado.
- *Alternativa más simple:* comprobar el permiso dentro de cada método. Duplica código y puede olvidarse.
- *Clases:* `ProxyAcceso`, `ServicioAdministracionUsuarios` (sujeto) y `ConfiguracionAdministracionDocentes` (lo envuelve).
- *SOLID:* SRP y OCP.
- *Beneficio:* autorización uniforme y declarativa.
- *Costo:* una indirección más.

**Chain of Responsibility (reutilizado y extendido)**
- *Problema:* archivar y eliminar comparten reglas (cuenta propia, último administrador), y eliminar añade el historial.
- *Razón:* `ValidadorBase<T>` ya existe en el proyecto. Cada regla es un eslabón independiente que se compone por operación.
- *Alternativa más simple:* una secuencia de `if` en el servicio. Mezcla reglas y dificulta reutilizarlas.
- *Clases:* `CadenasAdministracionCuentas` (ensambla), `ValidadorCuentaPropia`, `ValidadorAdministradorRestante` y `ValidadorSinRegistrosHistoricos`.
- *SOLID:* SRP (una regla por clase), OCP (nuevas reglas sin tocar el servicio) y LSP (eslabones intercambiables).
- *Costo:* más clases pequeñas.

**Observer (nuevo uso, mismo mecanismo que `ObservadorSesiones`)**
- *Problema:* auditar y avisar no debe ocurrir dentro de la transacción que bloquea `usuario`, ni acoplar el servicio a esos efectos.
- *Razón:* `@TransactionalEventListener(AFTER_COMMIT)` publica solo cambios confirmados y evita esperas de bloqueo.
- *Alternativa más simple:* llamar a `ServicioAvisos` directamente. Se descartó por el riesgo de bloqueo.
- *Clases:* `CuentaAdministradaEvento` y `ObservadorCuentasAdministradas`.
- *SOLID:* SRP y DIP.
- *Costo:* si la escritura de la auditoría falla después del commit, no revierte la operación.

**State (no se usa)**
- El estado de una cuenta es un booleano `activo` con dos transiciones protegidas en `Usuario`. Una jerarquía de estados sería sobreingeniería.

**Puerto y adaptador (DIP)**
- `RegistrosHistoricosUsuario` es la abstracción y `RegistrosHistoricosUsuarioJdbc` la implementación en `integracion`. El módulo de usuarios no conoce las tablas académicas.

### B — Limpieza de auditoría

No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID.

(`LimpiezaAuditoria` tiene una sola responsabilidad y reutiliza `ServicioAvisos` para su propio registro.)

### C — Botones uniformes

No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID.

### D — Ciclo de vida de casos

**Facade (reutilizado y extendido)**
- *Problema:* recuperar un caso exige comprobar el estado, revalidarlo, aplicar la transición y auditar.
- *Razón:* `FachadaPublicacionCasos` ya coordina publicar y archivar, así que `recuperar` se suma al mismo punto de entrada.
- *Alternativa más simple:* poner la lógica en el controlador. Rompe SRP.
- *Clases:* `FachadaPublicacionCasos`, `CasoEstudio`, `EstadoCaso` y `ServicioAvisos`.
- *SOLID:* SRP y OCP.

**Proxy (reutilizado)**
- `PublicacionCasos.recuperar` queda protegido por `ProxyAcceso` mediante `ConfiguracionServiciosCasos`.

**Ciclo de vida con enum (no es el patrón GoF State)**
- `EstadoCaso` se extendió con la transición ARCHIVADO → PUBLICADO y con las consultas `puedePublicarse()` y `puedeRecuperarse()`. El propio enum documenta que no usa una jerarquía de clases. Se mantiene esa decisión y no se presenta como patrón State.

**Builder (reutilizado indirectamente)**
- Duplicar sigue usando `ImportadorDsl` como director de `CasoEstudioBuilder`. No se modificó.

### E — Convocados con nombre y correo

No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID.

### Matriz

| Modificación | Patrón utilizado | Clases involucradas | Justificación | Principios SOLID |
|---|---|---|---|---|
| A. Autorización de administración de usuarios | Proxy (reutilizado) | `ProxyAcceso`, `ServicioAdministracionUsuarios`, `ConfiguracionAdministracionDocentes` | Permisos declarativos y fallo cerrado, igual que el resto del sistema | SRP, OCP |
| A. Reglas de archivar y eliminar | Chain of Responsibility (reutilizado y extendido) | `ValidadorBase`, `CadenasAdministracionCuentas`, `ValidadorCuentaPropia`, `ValidadorAdministradorRestante`, `ValidadorSinRegistrosHistoricos` | Reglas independientes y combinables por operación | SRP, OCP, LSP |
| A. Auditoría y avisos de cuentas | Observer (nuevo uso del mecanismo existente) | `CuentaAdministradaEvento`, `ObservadorCuentasAdministradas`, `ServicioAvisos` | Efectos tras el commit, sin acoplamiento ni esperas de bloqueo | SRP, DIP |
| A. Detección de historial | Ninguno (puerto y adaptador por DIP) | `RegistrosHistoricosUsuario`, `RegistrosHistoricosUsuarioJdbc` | Separa usuarios de las tablas académicas | DIP, ISP |
| B. Limpieza de auditoría | Ninguno | `LimpiezaAuditoria`, `ServicioAvisos`, `AvisoControlador` | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | SRP |
| C. Botones uniformes | Ninguno | `styles.css` y plantillas | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | — |
| D. Recuperar caso | Facade (reutilizado y extendido) y Proxy (reutilizado) | `FachadaPublicacionCasos`, `PublicacionCasos`, `CasoEstudio`, `EstadoCaso` | Un solo punto de coordinación, protegido por permiso | SRP, OCP |
| D. Transición ARCHIVADO → PUBLICADO | Ninguno (ciclo de vida con enum, no es GoF State) | `EstadoCaso`, `CasoEstudio` | Extiende la estructura existente | OCP |
| D. Reutilizar (Duplicar) | Builder (reutilizado, sin cambios) | `ImportadorDsl`, `CasoEstudioBuilder` | Funcionalidad existente | SRP |
| E. Nombre y correo de convocados | Ninguno | `sesion-detalle.ts`, `styles.css` | No requiere patrón de diseño adicional. La solución se implementó mediante las estructuras existentes, respetando los principios SOLID | — |
