import { SourceInvestigationDef } from './investigation.types';

export const HOSPITAL_SOURCES: SourceInvestigationDef[] = [
  {
    sourceId: 'madre',
    kind: 'npc',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [
          { speaker: 'Madre', text: 'No sé exactamente cómo empezó… Hubo una situación muy complicada en la casa.' },
        ],
        clueIds: ['h-madre-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Madre', text: 'Fue la primera vez que algo así pasó. Fue un incidente aislado, estamos todos muy alterados.' },
        ],
        clueIds: ['h-madre-t2'],
        requiresMinClues: 2,
      },
      {
        tier: 3,
        lines: [
          { speaker: 'Madre', text: 'Sí, a veces había gritos, pero no creo que esto sea tan grave como dicen. Prefiero que primero hablemos en familia.' },
        ],
        clueIds: ['h-madre-t3'],
        requiresClueIds: ['h-hermano-t2'],
      },
    ],
  },
  {
    sourceId: 'hermano',
    kind: 'npc',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Hermano', text: 'Llegué cuando ya estaba todo muy tenso. No vi cómo comenzó.' }],
        clueIds: ['h-hermano-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Hermano', text: 'No fue la primera vez. Ya había discusiones fuertes y amenazas desde hace meses.' },
        ],
        clueIds: ['h-hermano-t2'],
        requiresMinClues: 2,
      },
      {
        tier: 3,
        lines: [
          { speaker: 'Hermano', text: 'Vi cuando sacó algo punzante. Mi hermana solo pedía que se fuera; no provocó esto.' },
        ],
        clueIds: ['h-hermano-t3'],
        requiresClueIds: ['h-medico-t2'],
      },
    ],
  },
  {
    sourceId: 'medico',
    kind: 'npc',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [
          { speaker: 'Médico', text: 'Paciente en urgencias. Aún estamos estabilizando; el contexto del hogar no está completo.' },
        ],
        clueIds: ['h-medico-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Médico', text: 'Hay lesiones que preocupan. Necesitamos valoración de riesgo antes de cualquier entrevista profunda.' },
        ],
        clueIds: ['h-medico-t2'],
        requiresMinClues: 1,
      },
      {
        tier: 3,
        lines: [
          { speaker: 'Médico', text: 'Algunos hallazgos podrían ser compatibles con episodios previos; no es mi rol determinar culpabilidad.' },
        ],
        clueIds: ['h-medico-t3'],
        requiresClueIds: ['h-historia-t2'],
      },
    ],
  },
  {
    sourceId: 'enfermeria',
    kind: 'npc',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Enfermería', text: 'La paciente está muy asustada. Pide que controlen quién entra a la sala.' }],
        clueIds: ['h-enfermeria-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Enfermería', text: 'En sala de espera escuché discusiones sobre una niña pequeña y que “no puede volver a pasar”.' },
        ],
        clueIds: ['h-enfermeria-t2'],
        requiresMinClues: 3,
      },
    ],
  },
  {
    sourceId: 'guardia',
    kind: 'npc',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Guardia', text: 'Ingresó un grupo alterado. No alcancé a ver quién hizo qué.' }],
        clueIds: ['h-guardia-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Guardia', text: 'Antes del ingreso escuché amenazas en el pasillo, pero no tengo nombres claros.' },
        ],
        clueIds: ['h-guardia-t2'],
        requiresMinClues: 2,
      },
    ],
  },
  {
    sourceId: 'historia-clinica',
    kind: 'object',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Historia clínica', text: 'Ingreso reciente. Mecanismo de lesión en documentación preliminar.' }],
        clueIds: ['h-historia-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Historia clínica', text: 'Nota: posible agresión en contexto de convivencia. Se sugiere alerta de riesgo.' },
        ],
        clueIds: ['h-historia-t2'],
        requiresMinClues: 2,
      },
    ],
  },
  {
    sourceId: 'protocolo-epicee',
    kind: 'object',
    sceneId: 'hospital',
    tiers: [
      {
        tier: 1,
        lines: [
          { speaker: 'EPICEE', text: 'Protocolo en mesa: evaluación, protección, intervención y coordinación.' },
        ],
        clueIds: ['h-epicee-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'EPICEE', text: 'Checklist: amenaza a la vida, menores expuestos, acceso a medios de agresión.' },
        ],
        clueIds: ['h-epicee-t2'],
        requiresMinClues: 3,
      },
    ],
  },
];
