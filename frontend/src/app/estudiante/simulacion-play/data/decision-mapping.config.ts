import { SceneId } from './scene-types';

/** Palabras clave para localizar escenarios cuando el orden numérico no coincide. */
export const SCENE_BACKEND_MATCHERS: Record<
  SceneId,
  { escenarioOrdenPreferido: number; tituloKeywords: string[]; narrativaKeywords: string[] }
> = {
  hospital: {
    escenarioOrdenPreferido: 1,
    tituloKeywords: ['hospital', 'urgencias', 'urgencia', 'salud', 'médic', 'medico', 'epicee'],
    narrativaKeywords: ['hospital', 'urgencias', 'epicee', 'paciente'],
  },
  comisaria: {
    escenarioOrdenPreferido: 2,
    tituloKeywords: ['comisaría', 'comisaria', 'familia', 'protección', 'proteccion', 'cf'],
    narrativaKeywords: ['comisaría', 'comisaria', 'medida', 'expediente'],
  },
};

/**
 * Palabras clave por opción narrativa para emparejar respuesta.texto del backend
 * cuando respuesta.orden no coincide.
 */
export const OPTION_BACKEND_KEYWORDS: Record<string, string[]> = {
  'protocolo-integral': ['protocolo', 'integral', 'epicee', 'atención integral', 'atencion integral'],
  'entrevista-familiar': ['entrevista', 'familiar', 'familia'],
  'esperar-valoracion': ['esperar', 'valoración', 'valoracion', 'médica', 'medica', 'estabilización'],
  'medida-inmediata': ['protección inmediata', 'proteccion inmediata', 'medida', 'inmediata', 'alojamiento'],
  'acompanamiento-gradual': ['acompañamiento', 'acompanamiento', 'gradual', 'psicosocial'],
  'conciliacion': ['conciliación', 'conciliacion', 'diálogo', 'dialogo'],
};
