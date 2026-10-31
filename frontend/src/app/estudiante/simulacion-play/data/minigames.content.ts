export interface TimelineEventDef {
  id: string;
  label: string;
  correctOrder: number;
}

export const HOSPITAL_TIMELINE_EVENTS: TimelineEventDef[] = [
  { id: 'e1', label: 'Amenazas y control en el hogar (semanas previas)', correctOrder: 1 },
  { id: 'e2', label: 'Agresión con arma blanca — derivación a urgencias', correctOrder: 2 },
  { id: 'e3', label: 'Estabilización médica y valoración EPICEE', correctOrder: 3 },
  { id: 'e4', label: 'Activación de ruta de protección y registro en comisaría', correctOrder: 4 },
];

export interface RouteStepDef {
  id: string;
  label: string;
  description: string;
  isRecommended: boolean;
}

export const CARE_ROUTE_STEPS: RouteStepDef[] = [
  {
    id: 's1',
    label: 'Contención y seguridad inmediata',
    description: 'Garantizar integridad de la víctima y la menor.',
    isRecommended: true,
  },
  {
    id: 's2',
    label: 'Valoración psicosocial',
    description: 'Identificar factores de riesgo y redes de apoyo.',
    isRecommended: true,
  },
  {
    id: 's3',
    label: 'Medida de protección formal',
    description: 'Documentar y activar medidas ante comisaría.',
    isRecommended: true,
  },
  {
    id: 's4',
    label: 'Conciliación sin evaluación previa',
    description: 'Reunir a las partes sin análisis de riesgo.',
    isRecommended: false,
  },
  {
    id: 's5',
    label: 'Seguimiento interinstitucional',
    description: 'Coordinar salud, familia y justicia.',
    isRecommended: true,
  },
];

export interface RiskCardDef {
  id: string;
  text: string;
  kind: 'risk' | 'protection';
}

export const RISK_CLASSIFICATION_CARDS: RiskCardDef[] = [
  { id: 'c1', text: 'Control económico del agresor', kind: 'risk' },
  { id: 'c2', text: 'Alojamiento temporal seguro', kind: 'protection' },
  { id: 'c3', text: 'Amenazas reiteradas', kind: 'risk' },
  { id: 'c4', text: 'Acompañamiento en denuncia', kind: 'protection' },
  { id: 'c5', text: 'Menor expuesta en la vivienda', kind: 'risk' },
  { id: 'c6', text: 'Restricción de contacto con agresor', kind: 'protection' },
];
