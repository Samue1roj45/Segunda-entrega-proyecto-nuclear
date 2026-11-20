/** Formas de los datos que devuelve el backend (deben coincidir con sus DTO). */

export interface Rango { min: number | null; max: number | null }
export interface Respuesta {
  id: number; codigo: string; texto: string; esCorrecta: boolean | null; porcentaje: number | null; justificacion: string | null;
}
export interface Pregunta { id: number; enunciado: string; orden: number; respuestas: Respuesta[] }
export interface Escenario { id: number; nombre: string; contexto: string; orden: number; preguntas: Pregunta[] }
export interface Criterio { id: number; criterio: string; parametros: string[]; rango: Rango }
export interface Rubrica { id: number; nombre: string; descripcion: string | null; criteriosEvaluacion: Criterio[] }
export interface CasoDetalle {
  id: number; docenteId: number; nombre: string; desarrolloSituacional: string | null; contextoHistoria: string | null;
  tiempoEstimadoMinutos: number | null; estado: 'BORRADOR' | 'PUBLICADO' | 'ARCHIVADO';
  fechaCreacion: string | null; fechaActualizacion: string | null; fechaPublicacion: string | null; fechaArchivado: string | null;
  escenarios: Escenario[]; rubrica: Rubrica | null;
}
export interface DatosCaso {
  nombre: string; desarrolloSituacional: string; contextoHistoria: string; tiempoEstimadoMinutos: number | null;
}
export interface Incumplimiento { regla: string; mensaje: string; seccion: string | null; ubicacion: string | null }
export interface InformeValidacion { valido: boolean; incumplimientos: Incumplimiento[] }
export interface CasoEliminado { id: number; nombre: string; estado: string; fechaEliminacion: string }
export interface ResultadoImportacion { caso: CasoDetalle; advertencias: string[] }

export type TipoHerramienta = 'CALCULADORA' | 'TABLA' | 'DOCUMENTO' | 'ENLACE' | 'OTRA';
export interface Herramienta { idHerramienta: number; nombre: string; tipo: TipoHerramienta; descripcion: string | null; contenido: string | null; orden: number }

export interface SesionDetalle {
  idPractica: number; idCaso: number; nombreCaso: string; espacioAcademico: string;
  fechaInicio: string; fechaFin: string; tiempoMaximoMinutos: number; lugar: string | null;
  mensajePersonalizado: string | null; urlAcceso: string | null;
  estado: 'SIN_INICIAR' | 'EN_CURSO' | 'FINALIZADO';
  fechaInicioReal: string | null; fechaCierreReal: string | null; tipoCierre: string | null;
  accionesPermitidas: string[];
}
export interface Convocado { idAutorizacion: number; idEstudiante: number; nombre: string; correo: string; estado: string }
export interface SesionCreada { idPractica: number; convocados: Convocado[] }
export interface Intentos { idAutorizacion: number; intentosUsados: number; intentosPermitidos: number }
export interface AgendarSesion {
  idCaso: number; espacioAcademico: string; fechaInicio: string; fechaFin: string; tiempoMaximoMinutos: number;
  lugar: string | null; mensajePersonalizado: string | null; urlAcceso: string | null; idsEstudiantes: number[];
}

export interface Notificacion {
  id: number; tipo: string; practicaId: number; estudianteId: number | null; participacionId: number | null;
  destinatarioCorreo: string; asunto: string; cuerpo: string; estado: string; errorMensaje: string | null;
  fechaCreacion: string; fechaEnvio: string | null;
}
export interface ResumenEnvio { enviadas: number; omitidasDuplicado: number; errores: number }

export interface ParticipacionLista {
  id: number; practicaId: number; practicaNombre: string; estudianteId: number; estudianteCorreo: string;
  estudianteNombre: string; autorizacionId: number; numeroIntento: number; inicio: string | null; fin: string | null;
  tiempoUsadoSeg: number; estado: 'NO_INICIADA' | 'EN_CURSO' | 'FINALIZADA' | 'INCOMPLETA';
}
export interface DetallePregunta { preguntaId: number; enunciado: string; peso: number; respondida: boolean; respuestaElegidaId: number | null; correcta: boolean }
export interface ResultadoDetalle {
  id: number; participacionId: number; numeroIntento: number; estudianteId: number; estudianteCorreo: string; estudianteNombre: string;
  practicaId: number; practicaNombre: string; estadoParticipacion: string; correctas: number; incorrectas: number; noRespondidas: number;
  pesoObtenido: number; pesoTotal: number; escalaMaxima: number; notaFinal: number; oficial: boolean; notaOficial: number | null;
  fechaCalculo: string; detallePreguntas: DetallePregunta[];
}
export interface NotaOficial {
  id: number; practicaId: number; estudianteId: number; participacionId: number; numeroIntento: number;
  notaOficial: number; docenteId: number; fechaSeleccion: string;
}
export interface Retroalimentacion {
  id: number; participacionId: number; numeroIntento: number; estudianteId: number; practicaId: number; docenteId: number;
  texto: string; fechaCreacion: string; fechaActualizacion: string | null;
}
export interface PendientesEvaluadas { incompletas: number; ausencias: number; omitidas: number }

export interface Docente { id: number; correoInstitucional: string; nombreCompleto: string; activo: boolean; permisos: string[] }

/** Lo que ve el estudiante. */
export interface Convocatoria {
  idAutorizacion: number; idPractica: number; idCaso: number; nombreCaso: string; espacioAcademico: string;
  fechaInicio: string; fechaFin: string; tiempoMaximoMinutos: number; lugar: string | null;
  mensajePersonalizado: string | null; urlAcceso: string | null; estadoSesion: string; intentosUsados: number; intentosPermitidos: number;
}
export interface EstadoIngreso { permitido: boolean; motivo: string | null; limiteIngreso: string | null; ahora: string; estadoSesion: string }
export interface Opcion { idRespuesta: number; codigo: string; texto: string }
export interface PreguntaContexto { idPregunta: number; orden: number; enunciado: string; idRespuestaSeleccionada: number | null; opciones: Opcion[] }
export interface EscenarioContexto { idEscenario: number; orden: number; nombre: string; contexto: string; preguntas: PreguntaContexto[] }
export interface Contexto { nombreCaso: string; contextoHistoria: string | null; desarrolloSituacional: string | null; escenarios: EscenarioContexto[] }
export interface Progreso { idParticipacion: number; totalPreguntas: number; preguntasRespondidas: number; porcentajeProgreso: number }
export interface Tiempo { idParticipacion: number; estado: string; horaInicio: string; limite: string; segundosRestantes: number; expirada: boolean }
export interface Finalizacion { idParticipacion: number; estado: string; horaFin: string }

export interface EstudianteResumen { id: number; nombreCompleto: string; correoInstitucional: string }

/** Aviso dentro de la plataforma (campana). */
export interface Aviso { id: number; tipo: string; titulo: string; mensaje: string; leido: boolean; fecha: string }
export interface EntradaAuditoria { id: number; fecha: string; actorCorreo: string | null; accion: string; detalle: string }
