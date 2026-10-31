import { ClueDef } from './investigation.types';

export const CLUES_CATALOG: Record<string, ClueDef> = {
  // Hospital — NPCs
  'h-madre-t1': {
    id: 'h-madre-t1',
    sceneId: 'hospital',
    text: 'La madre afirma que hubo una situación muy tensa en la casa, sin dar muchos detalles.',
    unlockFactIds: ['f-conflicto-familiar'],
  },
  'h-madre-t2': {
    id: 'h-madre-t2',
    sceneId: 'hospital',
    text: 'La madre afirma que fue un incidente aislado, la primera vez que algo así escaló.',
    unlockTimelineIds: ['tl-discusion'],
  },
  'h-madre-t3': {
    id: 'h-madre-t3',
    sceneId: 'hospital',
    text: 'La madre reconoce gritos previos, pero minimiza la gravedad y pide no involucrar autoridades aún.',
  },
  'h-hermano-t1': {
    id: 'h-hermano-t1',
    sceneId: 'hospital',
    text: 'El hermano dice que llegó cuando ya había mucha tensión; no vio cómo empezó todo.',
  },
  'h-hermano-t2': {
    id: 'h-hermano-t2',
    sceneId: 'hospital',
    text: 'El hermano afirma que ya habían ocurrido discusiones fuertes y amenazas antes.',
    unlockFactIds: ['f-violencia-previa'],
    unlockTimelineIds: ['tl-escalada'],
  },
  'h-hermano-t3': {
    id: 'h-hermano-t3',
    sceneId: 'hospital',
    text: 'El hermano describe que el agresor sacó un objeto cortante; la víctima pedía que se fuera.',
    unlockTimelineIds: ['tl-lesion'],
  },
  'h-medico-t1': {
    id: 'h-medico-t1',
    sceneId: 'hospital',
    text: 'El médico indica que la paciente llegó en urgencias; el contexto familiar aún no está claro.',
    unlockTimelineIds: ['tl-urgencias'],
  },
  'h-medico-t2': {
    id: 'h-medico-t2',
    sceneId: 'hospital',
    text: 'El médico señala lesiones compatibles con agresión y solicita valoración de riesgo.',
    unlockFactIds: ['f-riesgo-repeticion'],
  },
  'h-medico-t3': {
    id: 'h-medico-t3',
    sceneId: 'hospital',
    text: 'El médico menciona señales que podrían indicar violencia recurrente, sin dictaminar culpabilidad.',
  },
  'h-enfermeria-t1': {
    id: 'h-enfermeria-t1',
    sceneId: 'hospital',
    text: 'Enfermería reporta que la paciente está asustada y pide que no entren más familiares sin aviso.',
  },
  'h-enfermeria-t2': {
    id: 'h-enfermeria-t2',
    sceneId: 'hospital',
    text: 'Enfermería escuchó a la familia discutir en sala de espera sobre “no volver a pasar”.',
    unlockFactIds: ['f-menor-presente'],
  },
  'h-guardia-t1': {
    id: 'h-guardia-t1',
    sceneId: 'hospital',
    text: 'El guardia recuerde que ingresó un grupo alterado; no pudo identificar bien quién agredió.',
  },
  'h-guardia-t2': {
    id: 'h-guardia-t2',
    sceneId: 'hospital',
    text: 'El guardia escuchó amenazas en el pasillo antes del ingreso a urgencias.',
    unlockTimelineIds: ['tl-escalada'],
  },
  // Hospital — objetos
  'h-historia-t1': {
    id: 'h-historia-t1',
    sceneId: 'hospital',
    text: 'La historia clínica registra ingreso por lesiones; mecanismo aún en verificación.',
    unlockTimelineIds: ['tl-urgencias'],
  },
  'h-historia-t2': {
    id: 'h-historia-t2',
    sceneId: 'hospital',
    text: 'El registro menciona conviviente masculino como posible agresor; hay nota de alerta.',
    unlockFactIds: ['f-violencia-previa'],
  },
  'h-epicee-t1': {
    id: 'h-epicee-t1',
    sceneId: 'hospital',
    text: 'El protocolo EPICEE está en mesa: evaluación de riesgo y coordinación interinstitucional.',
    unlockFactIds: ['f-ruta-proteccion'],
  },
  'h-epicee-t2': {
    id: 'h-epicee-t2',
    sceneId: 'hospital',
    text: 'EPICEE marca criterios de alerta: amenaza a la vida, menores expuestos, acceso a armas.',
    unlockFactIds: ['f-menor-presente', 'f-riesgo-repeticion'],
  },

  // Comisaría — NPCs (información nueva)
  'c-victima-t1': {
    id: 'c-victima-t1',
    sceneId: 'comisaria',
    text: 'La víctima dice que tiene miedo de hablar; no quiere regresar al mismo lugar.',
  },
  'c-victima-t2': {
    id: 'c-victima-t2',
    sceneId: 'comisaria',
    text: 'La víctima afirma que nunca había denunciado formalmente, por miedo a represalias.',
    unlockTimelineIds: ['tl-intervencion'],
  },
  'c-victima-t3': {
    id: 'c-victima-t3',
    sceneId: 'comisaria',
    text: 'La víctima pide protección para la menor que presenció la situación.',
    unlockFactIds: ['f-menor-presente'],
  },
  'c-comisario-t1': {
    id: 'c-comisario-t1',
    sceneId: 'comisaria',
    text: 'El comisario indica que el caso llegó derivado de urgencias; falta completar valoración.',
  },
  'c-comisario-t2': {
    id: 'c-comisario-t2',
    sceneId: 'comisaria',
    text: 'El comisario señala que debe evaluarse medida de protección según riesgo documentado.',
    unlockFactIds: ['f-ruta-proteccion'],
  },
  'c-tsocial-t1': {
    id: 'c-tsocial-t1',
    sceneId: 'comisaria',
    text: 'Trabajo social describe red de apoyo limitada en el territorio.',
  },
  'c-tsocial-t2': {
    id: 'c-tsocial-t2',
    sceneId: 'comisaria',
    text: 'Trabajo social propone alojamiento temporal y acompañamiento para la menor.',
    unlockFactIds: ['f-menor-presente'],
  },
  'c-secretaria-t1': {
    id: 'c-secretaria-t1',
    sceneId: 'comisaria',
    text: 'Secretaría confirma radicado reciente; hay trámites pendientes en el archivo.',
  },
  'c-secretaria-t2': {
    id: 'c-secretaria-t2',
    sceneId: 'comisaria',
    text: 'Secretaría localiza llamadas anteriores registradas en el sistema.',
    unlockTimelineIds: ['tl-intervencion'],
  },
  'c-proteccion-t1': {
    id: 'c-proteccion-t1',
    sceneId: 'comisaria',
    text: 'Protección indica que debe articularse seguimiento con salud y comisaría.',
  },
  'c-proteccion-t2': {
    id: 'c-proteccion-t2',
    sceneId: 'comisaria',
    text: 'Protección advierte que la demora en medidas aumenta exposición de la víctima.',
    unlockFactIds: ['f-riesgo-repeticion'],
  },
  // Comisaría — objetos
  'c-expediente-t1': {
    id: 'c-expediente-t1',
    sceneId: 'comisaria',
    text: 'El expediente muestra antecedentes de llamadas, sin detallar medidas aplicadas.',
  },
  'c-expediente-t2': {
    id: 'c-expediente-t2',
    sceneId: 'comisaria',
    text: 'El expediente registra una denuncia previa que la víctima dijo no haber hecho.',
    unlockFactIds: ['f-violencia-previa'],
  },
  'c-ruta-t1': {
    id: 'c-ruta-t1',
    sceneId: 'comisaria',
    text: 'La ruta de protección en pared describe cadena víctima–salud–comisaría–seguimiento.',
    unlockFactIds: ['f-ruta-proteccion'],
  },
  'c-reportes-t1': {
    id: 'c-reportes-t1',
    sceneId: 'comisaria',
    text: 'Reporte interinstitucional: lesiones y alerta de riesgo vital desde hospital.',
    unlockTimelineIds: ['tl-lesion'],
  },
  'c-denuncias-t1': {
    id: 'c-denuncias-t1',
    sceneId: 'comisaria',
    text: 'Archivo de denuncias: constancia de radicación anterior con fecha distinta al testimonio oral.',
  },
  'c-denuncias-t2': {
    id: 'c-denuncias-t2',
    sceneId: 'comisaria',
    text: 'La denuncia previa no tuvo medida de protección efectiva registrada.',
    unlockFactIds: ['f-ruta-proteccion'],
  },
};

export const ALL_CLUE_IDS = Object.keys(CLUES_CATALOG);
