import { HypothesisOption } from './investigation.types';

export const HYPOTHESIS_OPTIONS: HypothesisOption[] = [
  { id: 'hyp-insuficiente', label: 'Información insuficiente' },
  { id: 'hyp-moderado', label: 'Riesgo moderado' },
  { id: 'hyp-alto', label: 'Riesgo alto' },
  { id: 'hyp-recurrente', label: 'Violencia recurrente en el hogar' },
  { id: 'hyp-critico', label: 'Situación crítica / riesgo vital' },
];
