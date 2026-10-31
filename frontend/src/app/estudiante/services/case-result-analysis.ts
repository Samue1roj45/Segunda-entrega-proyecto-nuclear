import { DetallePreguntaResultado, Resultado } from '../../core/models/practicas.model';
import { SCENE_BACKEND_MATCHERS } from '../simulacion-play/data/decision-mapping.config';

export interface CaseClosureSnapshot {
  participacionId: number;
  personas: { nombre: string; rol: string }[];
  evidencias: { titulo: string }[];
  pistas: { text: string }[];
  contradicciones: { title: string }[];
  hechosMarcados: { label: string }[];
  timeline: { label: string }[];
  hypothesisHospital: string | null;
  hypothesisComisaria: string | null;
  decisions: { hospital?: string; comisaria?: string };
  clueCount: number;
}

export function loadClosureSnapshot(participacionId: number): CaseClosureSnapshot | null {
  const raw = sessionStorage.getItem(`simulacion-closure:${participacionId}`);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as CaseClosureSnapshot;
  } catch {
    return null;
  }
}

function matchesScene(enunciado: string, scene: 'hospital' | 'comisaria'): boolean {
  const lower = enunciado.toLowerCase();
  const keys = SCENE_BACKEND_MATCHERS[scene].tituloKeywords.concat(
    SCENE_BACKEND_MATCHERS[scene].narrativaKeywords,
  );
  return keys.some((k) => lower.includes(k.toLowerCase()));
}

export function decisionesPorEscenario(
  detalle: DetallePreguntaResultado[],
): { hospital: DetallePreguntaResultado[]; comisaria: DetallePreguntaResultado[]; otros: DetallePreguntaResultado[] } {
  const hospital: DetallePreguntaResultado[] = [];
  const comisaria: DetallePreguntaResultado[] = [];
  const otros: DetallePreguntaResultado[] = [];

  for (const d of detalle) {
    if (matchesScene(d.enunciado, 'hospital')) hospital.push(d);
    else if (matchesScene(d.enunciado, 'comisaria')) comisaria.push(d);
    else otros.push(d);
  }

  if (hospital.length === 0 && comisaria.length === 0 && detalle.length >= 2) {
    return { hospital: [detalle[0]], comisaria: [detalle[1]], otros: detalle.slice(2) };
  }

  return { hospital, comisaria, otros };
}

export function fortalezasYAreas(
  snapshot: CaseClosureSnapshot | null,
  resultado: Resultado,
): { fortalezas: string[]; areas: string[] } {
  const fortalezas: string[] = [];
  const areas: string[] = [];

  if (!snapshot) {
    areas.push('No se recuperó el resumen de investigación de la sesión.');
    return { fortalezas, areas };
  }

  if (snapshot.clueCount >= 10) {
    fortalezas.push('Buena capacidad investigativa: reuniste múltiples pistas.');
  } else if (snapshot.clueCount < 6) {
    areas.push('Exploración insuficiente: pocas pistas registradas en el expediente.');
  }

  if (snapshot.contradicciones.length >= 2) {
    fortalezas.push('Adecuado análisis de contradicciones entre fuentes.');
  } else if (snapshot.contradicciones.length === 0) {
    areas.push('No se detectaron contradicciones en tu expediente; revisa versiones cruzadas.');
  }

  if (snapshot.hechosMarcados.length >= 2) {
    fortalezas.push('Confirmación activa de hechos relevantes del caso.');
  } else {
    areas.push('Confirmación insuficiente de hechos en el expediente.');
  }

  const evidenciasH = snapshot.evidencias.length;
  if (evidenciasH >= 3) {
    fortalezas.push('Exploración documental adecuada.');
  } else {
    areas.push('Falta de exploración documental (evidencias insuficientes).');
  }

  if (resultado.correctas >= resultado.incorrectas && resultado.correctas > 0) {
    fortalezas.push('Decisiones profesionales mayoritariamente alineadas con el caso.');
  }
  if (resultado.incorrectas > 0) {
    areas.push('Revisa las oportunidades de mejora en tus decisiones institucionales.');
  }

  if (fortalezas.length === 0) {
    fortalezas.push('Completaste la investigación y el cierre del caso.');
  }

  return { fortalezas, areas };
}
