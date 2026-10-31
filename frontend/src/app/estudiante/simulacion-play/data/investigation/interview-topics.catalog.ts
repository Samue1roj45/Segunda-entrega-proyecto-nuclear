import { SceneId } from '../scene-types';
import { DialogueTier } from './investigation.types';

export interface InterviewTopicDef {
  id: string;
  npcId: string;
  sceneId: SceneId;
  label: string;
  lines: { speaker?: string; text: string }[];
  clueIds: string[];
  tier: DialogueTier;
  requiresMinClues?: number;
  requiresClueIds?: string[];
  requiresTopicIds?: string[];
}

const hospitalMadre: InterviewTopicDef[] = [
  {
    id: 'madre-hechos',
    npcId: 'madre',
    sceneId: 'hospital',
    label: 'Preguntar por los hechos',
    lines: [
      { speaker: 'Madre', text: 'No sé exactamente cómo empezó… Hubo una situación muy complicada en la casa.' },
    ],
    clueIds: ['h-madre-t1'],
    tier: 1,
  },
  {
    id: 'madre-antecedentes',
    npcId: 'madre',
    sceneId: 'hospital',
    label: 'Preguntar por antecedentes',
    lines: [
      { speaker: 'Madre', text: 'Fue la primera vez que algo así pasó. Fue un incidente aislado, estamos todos muy alterados.' },
    ],
    clueIds: ['h-madre-t2'],
    tier: 2,
    requiresMinClues: 2,
    requiresTopicIds: ['madre-hechos'],
  },
  {
    id: 'madre-relacion',
    npcId: 'madre',
    sceneId: 'hospital',
    label: 'Preguntar por la relación',
    lines: [
      { speaker: 'Madre', text: 'Sí, a veces había gritos, pero no creo que esto sea tan grave como dicen. Prefiero que primero hablemos en familia.' },
    ],
    clueIds: ['h-madre-t3'],
    tier: 3,
    requiresTopicIds: ['madre-antecedentes'],
    requiresClueIds: ['h-hermano-t2'],
  },
  {
    id: 'madre-menor',
    npcId: 'madre',
    sceneId: 'hospital',
    label: 'Preguntar por la menor',
    lines: [
      { speaker: 'Madre', text: 'Mi hija está con una tía. No quiero que la asusten con preguntas; es muy pequeña.' },
    ],
    clueIds: [],
    tier: 2,
    requiresTopicIds: ['madre-hechos'],
  },
];

export const HOSPITAL_INTERVIEW_TOPICS: InterviewTopicDef[] = [
  ...hospitalMadre,
  {
    id: 'hermano-hechos',
    npcId: 'hermano',
    sceneId: 'hospital',
    label: 'Preguntar por los hechos',
    lines: [{ speaker: 'Hermano', text: 'Llegué cuando ya estaba todo muy tenso. No vi cómo comenzó.' }],
    clueIds: ['h-hermano-t1'],
    tier: 1,
  },
  {
    id: 'hermano-antecedentes',
    npcId: 'hermano',
    sceneId: 'hospital',
    label: 'Preguntar por antecedentes',
    lines: [
      { speaker: 'Hermano', text: 'No fue la primera vez. Ya había discusiones fuertes y amenazas desde hace meses.' },
    ],
    clueIds: ['h-hermano-t2'],
    tier: 2,
    requiresMinClues: 2,
    requiresTopicIds: ['hermano-hechos'],
  },
  {
    id: 'hermano-relacion',
    npcId: 'hermano',
    sceneId: 'hospital',
    label: 'Preguntar por la relación en el hogar',
    lines: [
      { speaker: 'Hermano', text: 'Vi cuando sacó algo punzante. Mi hermana solo pedía que se fuera; no provocó esto.' },
    ],
    clueIds: ['h-hermano-t3'],
    tier: 3,
    requiresTopicIds: ['hermano-antecedentes'],
    requiresClueIds: ['h-medico-t2'],
  },
  {
    id: 'hermano-menor',
    npcId: 'hermano',
    sceneId: 'hospital',
    label: 'Preguntar por la menor',
    lines: [
      { speaker: 'Hermano', text: 'La niña estaba llorando en el pasillo. La sacamos de la sala lo antes posible.' },
    ],
    clueIds: [],
    tier: 1,
    requiresTopicIds: ['hermano-hechos'],
  },
  {
    id: 'medico-hechos',
    npcId: 'medico',
    sceneId: 'hospital',
    label: 'Preguntar por los hechos clínicos',
    lines: [
      { speaker: 'Médico', text: 'Paciente en urgencias. Aún estamos estabilizando; el contexto del hogar no está completo.' },
    ],
    clueIds: ['h-medico-t1'],
    tier: 1,
  },
  {
    id: 'medico-riesgo',
    npcId: 'medico',
    sceneId: 'hospital',
    label: 'Preguntar por riesgo y lesiones',
    lines: [
      { speaker: 'Médico', text: 'Hay lesiones que preocupan. Necesitamos valoración de riesgo antes de cualquier entrevista profunda.' },
    ],
    clueIds: ['h-medico-t2'],
    tier: 2,
    requiresMinClues: 1,
    requiresTopicIds: ['medico-hechos'],
  },
  {
    id: 'medico-antecedentes',
    npcId: 'medico',
    sceneId: 'hospital',
    label: 'Preguntar por antecedentes médicos',
    lines: [
      { speaker: 'Médico', text: 'Algunos hallazgos podrían ser compatibles con episodios previos; no es mi rol determinar culpabilidad.' },
    ],
    clueIds: ['h-medico-t3'],
    tier: 3,
    requiresTopicIds: ['medico-riesgo'],
    requiresClueIds: ['h-historia-t2'],
  },
  {
    id: 'enfermeria-hechos',
    npcId: 'enfermeria',
    sceneId: 'hospital',
    label: 'Preguntar por la situación actual',
    lines: [{ speaker: 'Enfermería', text: 'La paciente está muy asustada. Pide que controlen quién entra a la sala.' }],
    clueIds: ['h-enfermeria-t1'],
    tier: 1,
  },
  {
    id: 'enfermeria-menor',
    npcId: 'enfermeria',
    sceneId: 'hospital',
    label: 'Preguntar por la menor y sala de espera',
    lines: [
      { speaker: 'Enfermería', text: 'En sala de espera escuché discusiones sobre una niña pequeña y que “no puede volver a pasar”.' },
    ],
    clueIds: ['h-enfermeria-t2'],
    tier: 2,
    requiresMinClues: 3,
    requiresTopicIds: ['enfermeria-hechos'],
  },
  {
    id: 'guardia-hechos',
    npcId: 'guardia',
    sceneId: 'hospital',
    label: 'Preguntar por el ingreso',
    lines: [{ speaker: 'Guardia', text: 'Ingresó un grupo alterado. No alcancé a ver quién hizo qué.' }],
    clueIds: ['h-guardia-t1'],
    tier: 1,
  },
  {
    id: 'guardia-antecedentes',
    npcId: 'guardia',
    sceneId: 'hospital',
    label: 'Preguntar por lo ocurrido antes del ingreso',
    lines: [
      { speaker: 'Guardia', text: 'Antes del ingreso escuché amenazas en el pasillo, pero no tengo nombres claros.' },
    ],
    clueIds: ['h-guardia-t2'],
    tier: 2,
    requiresMinClues: 2,
    requiresTopicIds: ['guardia-hechos'],
  },
];

export const COMISARIA_INTERVIEW_TOPICS: InterviewTopicDef[] = [
  {
    id: 'victima-hechos',
    npcId: 'victima',
    sceneId: 'comisaria',
    label: 'Preguntar por los hechos',
    lines: [{ speaker: 'Víctima', text: 'Tengo miedo… No quiero volver a ese lugar.' }],
    clueIds: ['c-victima-t1'],
    tier: 1,
  },
  {
    id: 'victima-antecedentes',
    npcId: 'victima',
    sceneId: 'comisaria',
    label: 'Preguntar por antecedentes',
    lines: [
      { speaker: 'Víctima', text: 'Nunca había denunciado. Tenía miedo de que la situación empeorara.' },
    ],
    clueIds: ['c-victima-t2'],
    tier: 2,
    requiresMinClues: 3,
    requiresTopicIds: ['victima-hechos'],
  },
  {
    id: 'victima-menor',
    npcId: 'victima',
    sceneId: 'comisaria',
    label: 'Preguntar por la menor',
    lines: [
      { speaker: 'Víctima', text: 'Mi hija pequeña vio lo que pasó. Necesito que la protejan a ella también.' },
    ],
    clueIds: ['c-victima-t3'],
    tier: 3,
    requiresTopicIds: ['victima-antecedentes'],
    requiresClueIds: ['c-reportes-t1'],
  },
  {
    id: 'comisario-hechos',
    npcId: 'comisario',
    sceneId: 'comisaria',
    label: 'Preguntar por el caso',
    lines: [{ speaker: 'Comisario', text: 'El caso llegó desde urgencias. Necesitamos tu lectura para definir medidas.' }],
    clueIds: ['c-comisario-t1'],
    tier: 1,
  },
  {
    id: 'comisario-medidas',
    npcId: 'comisario',
    sceneId: 'comisaria',
    label: 'Preguntar por medidas de protección',
    lines: [
      { speaker: 'Comisario', text: 'Con el riesgo documentado, debe evaluarse activación de protección formal.' },
    ],
    clueIds: ['c-comisario-t2'],
    tier: 2,
    requiresMinClues: 4,
    requiresTopicIds: ['comisario-hechos'],
  },
  {
    id: 'tsocial-hechos',
    npcId: 'trabajador-social',
    sceneId: 'comisaria',
    label: 'Preguntar por el contexto familiar',
    lines: [{ speaker: 'Trabajador social', text: 'La red de apoyo en el barrio es limitada; el caso requiere seguimiento cercano.' }],
    clueIds: ['c-tsocial-t1'],
    tier: 1,
  },
  {
    id: 'tsocial-apoyo',
    npcId: 'trabajador-social',
    sceneId: 'comisaria',
    label: 'Preguntar por apoyo y alojamiento',
    lines: [
      { speaker: 'Trabajador social', text: 'Propongo alojamiento temporal y acompañamiento para la menor y la sobreviviente.' },
    ],
    clueIds: ['c-tsocial-t2'],
    tier: 2,
    requiresMinClues: 3,
    requiresTopicIds: ['tsocial-hechos'],
  },
  {
    id: 'secretaria-hechos',
    npcId: 'secretaria',
    sceneId: 'comisaria',
    label: 'Preguntar por trámites del caso',
    lines: [{ speaker: 'Secretaría', text: 'Hay un radicado reciente. Algunos trámites siguen en archivo.' }],
    clueIds: ['c-secretaria-t1'],
    tier: 1,
  },
  {
    id: 'secretaria-antecedentes',
    npcId: 'secretaria',
    sceneId: 'comisaria',
    label: 'Preguntar por registros anteriores',
    lines: [
      { speaker: 'Secretaría', text: 'En el sistema aparecen llamadas anteriores registradas en fechas distintas.' },
    ],
    clueIds: ['c-secretaria-t2'],
    tier: 2,
    requiresMinClues: 2,
    requiresTopicIds: ['secretaria-hechos'],
  },
  {
    id: 'proteccion-hechos',
    npcId: 'proteccion',
    sceneId: 'comisaria',
    label: 'Preguntar por coordinación',
    lines: [{ speaker: 'Protección', text: 'Debe coordinarse seguimiento con salud y comisaría sin demoras innecesarias.' }],
    clueIds: ['c-proteccion-t1'],
    tier: 1,
  },
  {
    id: 'proteccion-urgencia',
    npcId: 'proteccion',
    sceneId: 'comisaria',
    label: 'Preguntar por urgencia de medidas',
    lines: [
      { speaker: 'Protección', text: 'Cada hora sin medida aumenta la exposición de la víctima y de quienes dependen de ella.' },
    ],
    clueIds: ['c-proteccion-t2'],
    tier: 2,
    requiresMinClues: 4,
    requiresTopicIds: ['proteccion-hechos'],
  },
];

export function topicsForScene(sceneId: SceneId): InterviewTopicDef[] {
  return sceneId === 'hospital' ? HOSPITAL_INTERVIEW_TOPICS : COMISARIA_INTERVIEW_TOPICS;
}
