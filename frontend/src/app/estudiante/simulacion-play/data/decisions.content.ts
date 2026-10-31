import { SceneId } from './scene-types';

export interface NarrativeDelta {
  confianzaFamiliar: number;
  proteccionDerechos: number;
  calidadIntervencion: number;
  riesgoRevictimizacion: number;
}

export interface DecisionOptionDef {
  id: string;
  icon: string;
  actionTitle: string;
  actionSubtitle: string;
  /** Orden de respuesta en el backend (1-based) dentro de la pregunta del escenario. */
  respuestaOrden: number;
  consequenceTitle: string;
  consequenceLines: string[];
  narrativeFeedback: string;
  delta: NarrativeDelta;
}

export interface SceneDecisionDef {
  sceneId: SceneId;
  escenarioOrden: number;
  preguntaOrden: number;
  prompt: string;
  context: string;
  options: DecisionOptionDef[];
}

export const HOSPITAL_DECISION: SceneDecisionDef = {
  sceneId: 'hospital',
  escenarioOrden: 1,
  preguntaOrden: 1,
  prompt: '¿Qué acción profesional realizarás ahora?',
  context:
    'Revisaste testimonios, historia clínica y protocolo EPICEE. El equipo espera tu criterio para coordinar la siguiente intervención.',
  options: [
    {
      id: 'protocolo-integral',
      icon: 'medical_services',
      actionTitle: 'Activar protocolo de atención integral',
      actionSubtitle: 'Coordinar EPICEE, seguridad y derivación inmediata',
      respuestaOrden: 1,
      consequenceTitle: 'Ruta integral activada',
      consequenceLines: [
        'El equipo médico y trabajo social alinean criterios bajo EPICEE.',
        'Se prioriza seguridad de la menor y se evita revictimización en la entrevista.',
      ],
      narrativeFeedback:
        'La intervención articula salud, protección y derechos. La familia percibe contención, aunque el proceso genera tensión inicial.',
      delta: {
        confianzaFamiliar: 5,
        proteccionDerechos: 12,
        calidadIntervencion: 14,
        riesgoRevictimizacion: -10,
      },
    },
    {
      id: 'entrevista-familiar',
      icon: 'groups',
      actionTitle: 'Priorizar entrevista familiar',
      actionSubtitle: 'Profundizar en dinámicas del hogar antes de derivar',
      respuestaOrden: 2,
      consequenceTitle: 'Enfoque centrado en la familia',
      consequenceLines: [
        'La madre y el hermano aportan más contexto sobre amenazas previas.',
        'La derivación a protección se retrasa algunas horas.',
      ],
      narrativeFeedback:
        'Ganas confianza con el núcleo familiar, pero el tiempo sin medidas formales aumenta la exposición de la menor.',
      delta: {
        confianzaFamiliar: 14,
        proteccionDerechos: 2,
        calidadIntervencion: 6,
        riesgoRevictimizacion: 8,
      },
    },
    {
      id: 'esperar-valoracion',
      icon: 'hourglass_top',
      actionTitle: 'Esperar nueva valoración médica',
      actionSubtitle: 'Postergar decisiones hasta estabilización completa',
      respuestaOrden: 3,
      consequenceTitle: 'Intervención en pausa',
      consequenceLines: [
        'El equipo clínico continúa procedimientos sin activar ruta social aún.',
        'La familia interpreta silencio institucional como abandono.',
      ],
      narrativeFeedback:
        'La calma clínica es comprensible, pero el vacío de acción social eleva el riesgo de revictimización y desconfianza.',
      delta: {
        confianzaFamiliar: -10,
        proteccionDerechos: -6,
        calidadIntervencion: -4,
        riesgoRevictimizacion: 12,
      },
    },
  ],
};

export const COMISARIA_DECISION: SceneDecisionDef = {
  sceneId: 'comisaria',
  escenarioOrden: 2,
  preguntaOrden: 1,
  prompt: '¿Cómo procederás en la Comisaría de Familia?',
  context:
    'Con expediente, ruta de protección y documentos revisados, debes definir la medida que mejor articule derechos y seguridad.',
  options: [
    {
      id: 'medida-inmediata',
      icon: 'shield',
      actionTitle: 'Activar medida de protección inmediata',
      actionSubtitle: 'Alojamiento seguro y restricción de contacto con agresor',
      respuestaOrden: 1,
      consequenceTitle: 'Protección activada',
      consequenceLines: [
        'Se documenta riesgo alto y se notifica a autoridades competentes.',
        'La víctima recibe acompañamiento para trámites urgentes.',
      ],
      narrativeFeedback:
        'La decisión refuerza derechos y reduce exposición. La familia extensa puede resistirse al inicio.',
      delta: {
        confianzaFamiliar: 0,
        proteccionDerechos: 16,
        calidadIntervencion: 12,
        riesgoRevictimizacion: -12,
      },
    },
    {
      id: 'acompanamiento-gradual',
      icon: 'handshake',
      actionTitle: 'Acompañamiento psicosocial gradual',
      actionSubtitle: 'Seguimiento cercano sin medida restrictiva aún',
      respuestaOrden: 2,
      consequenceTitle: 'Seguimiento pactado',
      consequenceLines: [
        'Trabajo social agenda visitas y plan de autocuidado.',
        'El agresor aún no tiene restricciones formales.',
      ],
      narrativeFeedback:
        'La víctima se siente escuchada, pero la ausencia de medidas firmes mantiene un riesgo residual elevado.',
      delta: {
        confianzaFamiliar: 8,
        proteccionDerechos: 6,
        calidadIntervencion: 8,
        riesgoRevictimizacion: 4,
      },
    },
    {
      id: 'conciliacion',
      icon: 'balance',
      actionTitle: 'Proponer espacio de conciliación familiar',
      actionSubtitle: 'Invitar diálogo entre partes antes de medidas',
      respuestaOrden: 3,
      consequenceTitle: 'Tensión en la sala',
      consequenceLines: [
        'La víctima se retrae; el comisario advierte incompatibilidad con el contexto de violencia.',
        'El expediente queda abierto sin cierre de riesgo.',
      ],
      narrativeFeedback:
        'La propuesta minimiza la gravedad del caso y aumenta revictimización. El equipo documenta tu criterio para revisión académica.',
      delta: {
        confianzaFamiliar: -8,
        proteccionDerechos: -14,
        calidadIntervencion: -10,
        riesgoRevictimizacion: 16,
      },
    },
  ],
};
