import { SourceInvestigationDef } from './investigation.types';

export const COMISARIA_SOURCES: SourceInvestigationDef[] = [
  {
    sourceId: 'victima',
    kind: 'npc',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Víctima', text: 'Tengo miedo… No quiero volver a ese lugar.' }],
        clueIds: ['c-victima-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Víctima', text: 'Nunca había denunciado. Tenía miedo de que la situación empeorara.' },
        ],
        clueIds: ['c-victima-t2'],
        requiresMinClues: 3,
      },
      {
        tier: 3,
        lines: [
          { speaker: 'Víctima', text: 'Mi hija pequeña vio lo que pasó. Necesito que la protejan a ella también.' },
        ],
        clueIds: ['c-victima-t3'],
        requiresClueIds: ['c-reportes-t1'],
      },
    ],
  },
  {
    sourceId: 'comisario',
    kind: 'npc',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Comisario', text: 'El caso llegó desde urgencias. Necesitamos tu lectura para definir medidas.' }],
        clueIds: ['c-comisario-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Comisario', text: 'Con el riesgo documentado, debe evaluarse activación de protección formal.' },
        ],
        clueIds: ['c-comisario-t2'],
        requiresMinClues: 4,
      },
    ],
  },
  {
    sourceId: 'trabajador-social',
    kind: 'npc',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Trabajador social', text: 'La red de apoyo en el barrio es limitada; el caso requiere seguimiento cercano.' }],
        clueIds: ['c-tsocial-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Trabajador social', text: 'Propongo alojamiento temporal y acompañamiento para la menor y la sobreviviente.' },
        ],
        clueIds: ['c-tsocial-t2'],
        requiresMinClues: 3,
      },
    ],
  },
  {
    sourceId: 'secretaria',
    kind: 'npc',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Secretaría', text: 'Hay un radicado reciente. Algunos trámites siguen en archivo.' }],
        clueIds: ['c-secretaria-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Secretaría', text: 'En el sistema aparecen llamadas anteriores registradas en fechas distintas.' },
        ],
        clueIds: ['c-secretaria-t2'],
        requiresMinClues: 2,
      },
    ],
  },
  {
    sourceId: 'proteccion',
    kind: 'npc',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Protección', text: 'Debe coordinarse seguimiento con salud y comisaría sin demoras innecesarias.' }],
        clueIds: ['c-proteccion-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Protección', text: 'Cada hora sin medida aumenta la exposición de la víctima y de quienes dependen de ella.' },
        ],
        clueIds: ['c-proteccion-t2'],
        requiresMinClues: 4,
      },
    ],
  },
  {
    sourceId: 'expediente',
    kind: 'object',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Expediente', text: 'Antecedentes en archivo. Algunas actuaciones previas sin detalle de cierre.' }],
        clueIds: ['c-expediente-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Expediente', text: 'Consta una denuncia previa que no coincide con lo que algunos testimonios niegan.' },
        ],
        clueIds: ['c-expediente-t2'],
        requiresClueIds: ['c-victima-t2'],
      },
    ],
  },
  {
    sourceId: 'ruta-proteccion',
    kind: 'object',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [
          { speaker: 'Ruta', text: 'Diagrama: víctima → valoración → salud mental → comisaría → medida → seguimiento.' },
        ],
        clueIds: ['c-ruta-t1'],
      },
    ],
  },
  {
    sourceId: 'reportes',
    kind: 'object',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [
          { text: 'Reporte hospitalario: lesiones y alerta de riesgo. Nota de trabajo social sobre menor expuesta.' },
        ],
        clueIds: ['c-reportes-t1'],
      },
    ],
  },
  {
    sourceId: 'denuncias',
    kind: 'object',
    sceneId: 'comisaria',
    tiers: [
      {
        tier: 1,
        lines: [{ speaker: 'Denuncias', text: 'Radicación anterior con fecha registrada. No coincide con todas las versiones orales.' }],
        clueIds: ['c-denuncias-t1'],
      },
      {
        tier: 2,
        lines: [
          { speaker: 'Denuncias', text: 'No consta medida de protección efectiva aplicada tras la denuncia previa.' },
        ],
        clueIds: ['c-denuncias-t2'],
        requiresClueIds: ['c-expediente-t2'],
      },
    ],
  },
];
