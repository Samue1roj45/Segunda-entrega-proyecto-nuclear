export interface IntroNarrativaEscena {
  id: string;
  imageUrl: string;
  /** Gradiente de respaldo si la imagen falla */
  fallbackGradient: string;
  lines: string[];
}

export const INTRO_NARRATIVA_ESCENAS: IntroNarrativaEscena[] = [
  {
    id: 'barrio',
    imageUrl: '/assets/simulacion/intro/intro-01-barrio.svg',
    fallbackGradient: 'linear-gradient(160deg, #0f172a 0%, #1e3a5f 50%, #334155 100%)',
    lines: [
      '11:00 PM. Un barrio marcado por la pobreza estructural y la violencia urbana.',
      'Las calles están en silencio, pero no en paz.',
    ],
  },
  {
    id: 'conflictos',
    imageUrl: '/assets/simulacion/intro/intro-02-conflictos.svg',
    fallbackGradient: 'linear-gradient(160deg, #422006 0%, #7c2d12 45%, #1e293b 100%)',
    lines: [
      'En una vivienda, los conflictos familiares se acumulan desde hace tiempo.',
      'Gritos, control, miedo. Nadie desde afuera conoce el caso completo.',
    ],
  },
  {
    id: 'escalada',
    imageUrl: '/assets/simulacion/intro/intro-03-escalada.svg',
    fallbackGradient: 'linear-gradient(160deg, #450a0a 0%, #991b1b 40%, #1e1b4b 100%)',
    lines: [
      'La tensión escala en minutos.',
      'Lo que empezó como una discusión se convierte en una emergencia.',
    ],
  },
  {
    id: 'emergencia',
    imageUrl: '/assets/simulacion/intro/intro-04-emergencia.svg',
    fallbackGradient: 'linear-gradient(160deg, #1e1b4b 0%, #dc2626 35%, #0f172a 100%)',
    lines: [
      'Llegan los servicios de emergencia.',
      'Tu rol como profesional de psicología social comienza a tomar forma.',
    ],
  },
  {
    id: 'hospital',
    imageUrl: '/assets/simulacion/intro/intro-05-hospital.svg',
    fallbackGradient: 'linear-gradient(160deg, #0c4a6e 0%, #7dd3fc 40%, #e0f2fe 100%)',
    lines: [
      'Urgencias hospitalarias.',
      'Antes de decidir, deberás investigar: personas, documentos y versiones que no siempre coinciden.',
    ],
  },
];
