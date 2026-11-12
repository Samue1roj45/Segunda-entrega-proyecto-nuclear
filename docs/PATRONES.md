# SOMA · Patrones de diseño implementados

Cada clase listada lleva en el código un comentario `Patrón …` que explica su papel. Las rutas son relativas a
`<módulo>/backend/src/main/java/co/edu/humboldt/plataformacasos/` (o `aplicacion/src/main/java/...` para `integracion`).

## Chain of Responsibility

| Módulo | RF | Clases | Papel |
|---|---|---|---|
| M1 | RF-01..RF-03 | `compartido/validacion/Validador`, `ValidadorBase`, `ResultadoValidacion` | Contrato y base de los eslabones |
| M1 | RF-01..RF-03 | `autenticacion/validacion/ValidadorCamposObligatorios`, `ValidadorDominioInstitucional`, `ValidadorEstudianteRegistrado`, `ValidadorCodigoAutorizacion` | Eslabones del login |
| M1 | RF-01..RF-03 | `autenticacion/validacion/CadenasAutenticacionConfig` | Arma las cadenas de docente y estudiante |
| M2 | RF-09 | `casos/validacion/ReglaOcl`, `ValidadorDslCeps` | Reglas OCL que se revisan en orden antes de publicar |
| M3 | RF-04, RF-18 | `sesiones/servicio/validacion/ValidadorAgenda`, `ValidadorFechas`, `ValidadorTiempoMaximo`, `ValidadorEspacioAcademico`, `ValidadorEstudiantes`, `CadenaValidacionAgenda` | Validación de la agenda de una sesión |
| M4 | RF-25 | `sesiones/servicio/repeticion/ValidadorRepeticion`, `ValidadorSesionNoFinalizada`, `ValidadorDocenteCreador`, `ValidadorAutorizacionActiva`, `ValidadorLimiteRepeticiones`, `CadenaValidacionRepeticion`; mensaje `sesiones/dominio/SolicitudRepeticion` | Validación de un intento extra |

## Proxy (de protección)

| Módulo | RF | Clases | Papel |
|---|---|---|---|
| M1 | RF-02 | `autorizacion/ProxyAcceso` (proxy dinámico), `RequierePermiso` | Verifica el permiso declarado antes de llamar al servicio real |
| M1/M2 | RF-02, RF-05..RF-09, RF-17 | `ConfiguracionAdministracionDocentes`, `ConfiguracionServiciosCasos`, `ConfiguracionServiciosRubricas`, `ConfiguracionIntegracionCasos` | Publican cada servicio envuelto en `ProxyAcceso` |
| M4 | RF-10, RF-11, RF-23 | `sesiones/proxy/ProxyServicioParticipacionEstudiante`; interfaz `ServicioParticipacionEstudiante`; sujeto real `ServicioParticipacionEstudianteImpl` | Sólo el estudiante convocado entra y responde |
| M5-6 | RF-14..RF-16, RF-26 | `evaluacion/participacion/ProxyConsultaParticipacion`, `resultado/ProxyConsultaResultado`, `reporte/ProxyDescargaReporte`, `notificacion/ProxyRetroalimentacion` | Visibilidad por dueño, docente y permiso |

## State

| Módulo | RF | Clases | Papel |
|---|---|---|---|
| M3 | RF-19 | `sesiones/estado/EstadoSesion`, `SesionSinIniciar`, `SesionEnCurso`, `SesionFinalizada`, `EstadosSesion` (fábrica), `AccionesSesion`; contexto `servicio/ServicioGestionSesion` | Ciclo SIN_INICIAR → EN_CURSO → FINALIZADO |
| M5-6 | RF-12, RF-14 | `evaluacion/participacion/EstadoDeParticipacion`, `EstadoNoIniciada`, `EstadoEnCurso`, `EstadoFinalizada`, `EstadoIncompleta`, `MaquinaEstadoParticipacion` | Ciclo de cada intento |

M2 usa a propósito un `enum` (`casos/dominio/EstadoCaso`) en vez de State: el caso sólo pasa de borrador a publicado y
una jerarquía de clases no aportaría nada (está explicado en la propia clase).

## Strategy

| Módulo | RF | Clases | Papel |
|---|---|---|---|
| M4 | RF-22 | `sesiones/politica/PoliticaIngresoTardio`, `PoliticaMitadDelTiempo` (por defecto), `PoliticaPorcentajeConfigurable`; selección en `configuracion/ConfiguracionPoliticaIngreso` | Regla de ingreso tardío intercambiable por configuración |
| M5-6 | RF-13, RF-24 | `evaluacion/resultado/EstrategiaEvaluacion`, `EstrategiaEvaluacionCompleta`, `EstrategiaEvaluacionIncompleta`, `EstrategiaEvaluacionAusencia` | Cálculo de nota según cómo terminó el intento |

## Facade

| Módulo | RF | Clases |
|---|---|---|
| M2 | RF-09 | `casos/servicio/FachadaPublicacionCasos` |
| M3 | RF-04, RF-18, RF-19 | `sesiones/fachada/FachadaSesiones` |
| M5-6 | RF-12 | `evaluacion/participacion/FachadaParticipacion` |
| M5-6 | RF-13, RF-14, RF-24 | `evaluacion/resultado/FachadaEvaluacion` |
| M5-6 | RF-16 | `evaluacion/reporte/FachadaReportes` |
| M5-6 | RF-21, RF-27 | `evaluacion/notificacion/FachadaNotificaciones` |
| M5-6 | RF-26 | `evaluacion/notificacion/FachadaRetroalimentacion` |

## Builder

| Módulo | RF | Clases |
|---|---|---|
| M2 | RF-05..RF-08, RF-17 | `casos/dsl/CasoEstudioBuilder` |
| M3 | RF-04 | `sesiones/dominio/SesionProgramada` (builder interno) |

## Observer

| Módulo | RF | Clases | Papel |
|---|---|---|---|
| M3-4 | RF-11, RF-18, RF-19 | Eventos `sesiones/evento/EstudianteConvocadoEvento`, `SesionModificadaEvento`, `SesionFinalizadaEvento`, `RespuestaRegistradaEvento`; observador `ObservadorRegistroProgreso` | Avisos de sesión publicados con el bus de eventos de Spring |
| M5-6 | RF-21, RF-27 | Sujeto `evaluacion/notificacion/PublicadorEventos`; observadores `ObservadorAcademico`, `ObservadorCorreo` | Correos de convocatoria, cambios y calificaciones |
| Integración | RF-21, RF-24 | `aplicacion/.../integracion/ObservadorSesiones` | Escucha los eventos de M3 y dispara correos y cierres en M5 |

## Puertos y adaptadores (comunicación entre módulos)

| Puerto (dueño) | Adaptador (implementa) | Para qué |
|---|---|---|
| `autenticacion/puerto/VerificadorCodigoAutorizacion` (M1) | `sesiones/servicio/VerificadorCodigoAcceso` (M3) | Login del estudiante con su código |
| `casos/integracion/ConsultaCasosProgramables` (M2) | `casos/integracion/ServicioIntegracionCasos` (M2, detrás de `ProxyAcceso`) | M3 programa sesiones sobre casos publicados |
| `casos/integracion/ContenidoCasoInterno` (M2) | `casos/integracion/ServicioContenidoCaso` (M2) | M4/M5 leen preguntas y opciones |
| `sesiones/integracion/ConsultaSesiones` (M3) | `sesiones/integracion/ServicioConsultaSesiones` (M3) | Otros módulos leen prácticas y autorizaciones |
| `evaluacion/contrato/ActorActualResolver` (M5) | `integracion/ActorDesdeSesion` | Actor desde el JWT de M1 |
| `evaluacion/contrato/CatalogoAcademico` (M5) | `integracion/CatalogoAcademicoIntegrado` | Prácticas y autorizaciones desde M3 |
| `evaluacion/contrato/PoliticaInicioIntento` (M5) | `integracion/PoliticaInicioDesdeSesiones` | Regla de ingreso tardío de M4 |
| `evaluacion/participacion/RegistroParticipaciones` (M5) | `evaluacion/participacion/ServicioRegistroParticipaciones` | M4 registra respuestas y cierra intentos |
| Gateway `sesiones/repositorio/ParticipacionEstudianteRepositorio` (M4) | — | Acceso de M4 a los datos de participación |
