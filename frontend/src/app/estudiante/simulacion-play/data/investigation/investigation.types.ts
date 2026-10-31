import { SceneId } from '../scene-types';

export type PersonaEstado =
  | 'BLOQUEADO'
  | 'DESCUBIERTO'
  | 'ENTREVISTA_T1'
  | 'ENTREVISTA_T2'
  | 'ENTREVISTA_T3';

export type DialogueTier = 1 | 2 | 3;

export interface ClueDef {
  id: string;
  sceneId: SceneId;
  text: string;
  unlockTimelineIds?: string[];
  unlockFactIds?: string[];
}

export interface ContradictionDef {
  id: string;
  sceneId: SceneId;
  title: string;
  clueIdA: string;
  clueIdB: string;
  narrative: string;
}

export interface PersonaDef {
  id: string;
  sceneId: SceneId;
  nombre: string;
  rol: string;
  color: string;
  shortLabel: string;
}

export interface EvidenciaDef {
  id: string;
  sceneId: SceneId;
  titulo: string;
  descripcion: string;
}

export interface HechoCandidatoDef {
  id: string;
  sceneId: SceneId | 'global';
  label: string;
  unlockClueIds: string[];
}

export interface TimelineEventDef {
  id: string;
  label: string;
  /** Etiqueta horaria narrativa (solo visible al descubrir). */
  timeLabel?: string;
  unlockClueIds: string[];
  order: number;
}

export interface TierDialogueBlock {
  tier: DialogueTier;
  lines: { speaker?: string; text: string }[];
  clueIds: string[];
  requiresClueIds?: string[];
  requiresMinClues?: number;
}

export interface SourceInvestigationDef {
  sourceId: string;
  kind: 'npc' | 'object';
  sceneId: SceneId;
  tiers: TierDialogueBlock[];
}

export interface HypothesisOption {
  id: string;
  label: string;
}
