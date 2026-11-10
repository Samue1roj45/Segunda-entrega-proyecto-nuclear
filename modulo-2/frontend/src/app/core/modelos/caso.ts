/** Contratos de la API del Módulo 2. Deben coincidir con los DTO del backend. */
export type EstadoCaso = 'BORRADOR' | 'PUBLICADO' | 'ARCHIVADO';
export type SeccionCaso = 'DATOS' | 'ESCENARIOS' | 'RUBRICA';

export interface CasoResumen {
  id: number;
  nombre: string | null;
  tiempoEstimadoMinutos: number | null;
  estado: EstadoCaso;
  fechaActualizacion: string;
  fechaPublicacion: string | null;
}

export interface DatosCaso {
  nombre: string | null;
  desarrolloSituacional: string | null;
  contextoHistoria: string | null;
  tiempoEstimadoMinutos: number | null;
}

export interface DatosRespuesta {
  codigo: string | null;
  texto: string | null;
  esCorrecta: boolean | null;
  porcentaje: number | null;
  justificacion: string | null;
}

export interface Respuesta extends DatosRespuesta {
  id: number;
}

export interface Pregunta {
  id: number;
  enunciado: string | null;
  orden: number;
  respuestas: Respuesta[];
}

export interface Escenario {
  id: number;
  nombre: string | null;
  contexto: string | null;
  orden: number;
  preguntas: Pregunta[];
}

export interface DatosCriterio {
  criterio: string | null;
  parametros: string[];
  rango: { min: number | null; max: number | null };
}

export interface Criterio extends DatosCriterio {
  id: number;
}

export interface Rubrica {
  id: number;
  nombre: string | null;
  descripcion: string | null;
  criteriosEvaluacion: Criterio[];
}

export interface CasoDetalle extends DatosCaso {
  id: number;
  estado: EstadoCaso;
  fechaActualizacion: string;
  fechaPublicacion: string | null;
  fechaArchivado: string | null;
  escenarios: Escenario[];
  rubrica: Rubrica | null;
}

export interface Incumplimiento {
  regla: string;
  mensaje: string;
  seccion: SeccionCaso;
  ubicacion: string;
}

export interface InformeValidacion {
  valido: boolean;
  incumplimientos: Incumplimiento[];
}
