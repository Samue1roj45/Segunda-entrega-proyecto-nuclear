/** Presentación visual/narrativa (no altera diálogos académicos). */
export type CharacterMood = 'distress' | 'anger' | 'neutral' | 'formal' | 'reserved' | 'care';

export interface CharacterPresentation {
  nombreCompleto: string;
  rolCaso: string;
  emocion: string;
  mood: CharacterMood;
  portraitUrl: string;
}

const PORTRAIT_BASE = '/assets/simulacion/portraits';

export function portraitUrlFor(npcId: string): string {
  return `${PORTRAIT_BASE}/${npcId}.svg`;
}

function pres(
  npcId: string,
  data: Omit<CharacterPresentation, 'portraitUrl'>,
): CharacterPresentation {
  return { ...data, portraitUrl: portraitUrlFor(npcId) };
}

export const CHARACTER_PRESENTATION: Record<string, CharacterPresentation> = {
  madre: pres('madre', {
    nombreCompleto: 'Marta Gómez',
    rolCaso: 'Madre de la víctima',
    emocion: 'Desesperación y llanto',
    mood: 'distress',
  }),
  hermano: pres('hermano', {
    nombreCompleto: 'Carlos Gómez',
    rolCaso: 'Hermano de la víctima',
    emocion: 'Ira e impotencia',
    mood: 'anger',
  }),
  medico: pres('medico', {
    nombreCompleto: 'Dr. Andrés Rojas',
    rolCaso: 'Médico tratante · Urgencias',
    emocion: 'Profesionalismo prudente',
    mood: 'neutral',
  }),
  enfermeria: pres('enfermeria', {
    nombreCompleto: 'Enf. Laura Méndez',
    rolCaso: 'Enfermería de turno',
    emocion: 'Atención centrada en la paciente',
    mood: 'care',
  }),
  guardia: pres('guardia', {
    nombreCompleto: 'Roberto Vargas',
    rolCaso: 'Guardia de seguridad',
    emocion: 'Formalidad y reserva',
    mood: 'formal',
  }),
  victima: pres('victima', {
    nombreCompleto: 'Ana Gómez',
    rolCaso: 'Víctima · Superviviente',
    emocion: 'Miedo y agotamiento',
    mood: 'distress',
  }),
  comisario: pres('comisario', {
    nombreCompleto: 'Dr. Felipe Ordóñez',
    rolCaso: 'Comisario de familia',
    emocion: 'Autoridad institucional',
    mood: 'formal',
  }),
  'trabajador-social': pres('trabajador-social', {
    nombreCompleto: 'Diana Salazar',
    rolCaso: 'Trabajadora social',
    emocion: 'Empatía profesional',
    mood: 'care',
  }),
  secretaria: pres('secretaria', {
    nombreCompleto: 'Patricia Núñez',
    rolCaso: 'Secretaría administrativa',
    emocion: 'Neutralidad procedimental',
    mood: 'neutral',
  }),
  proteccion: pres('proteccion', {
    nombreCompleto: 'Miguel Herrera',
    rolCaso: 'Equipo de protección',
    emocion: 'Reserva y cautela',
    mood: 'reserved',
  }),
};

export function presentationFor(npcId: string, fallbackLabel: string, fallbackRole: string): CharacterPresentation {
  return (
    CHARACTER_PRESENTATION[npcId] ?? {
      nombreCompleto: fallbackLabel,
      rolCaso: fallbackRole,
      emocion: 'En entrevista',
      mood: 'neutral',
      portraitUrl: portraitUrlFor(npcId),
    }
  );
}
