/** Intro narrativa breve — Caso Psicología Social (antes del hospital). */

export interface IntroStep {
  title: string;
  lines: string[];
}

export const INTRO_STEPS: IntroStep[] = [
  {
    title: '11:00 PM — Barrio vulnerable',
    lines: [
      'Un barrio con pobreza estructural, violencia urbana y presencia de grupos armados.',
      'Los conflictos comunitarios tensionan la convivencia diaria.',
    ],
  },
  {
    title: 'La emergencia en la vivienda',
    lines: [
      'Un hombre de 28 años llega a su hogar. Su pareja tiene 22 años. Hay una menor de 3 años.',
      'Se desata una agresión con arma blanca en contexto de violencia intrafamiliar.',
    ],
  },
  {
    title: 'Tu rol profesional',
    lines: [
      'Eres el profesional de psicología social en urgencias hospitalarias.',
      'Antes de cualquier decisión debes investigar: hablar con personas clave e inspeccionar documentos.',
      'Explora el hospital y registra cada pista en tu bitácora.',
    ],
  },
];
