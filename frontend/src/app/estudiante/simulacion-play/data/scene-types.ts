import { DialogueLine } from './hospital-scene.content';

export type SceneId = 'hospital' | 'comisaria';

export interface ClueDef {
  category: string;
  title: string;
  summary: string;
}

export interface NpcWaypoint {
  x: number;
  y: number;
  pauseSec?: number;
}

export interface PlayNpcDef {
  id: string;
  label: string;
  shortLabel: string;
  role: string;
  x: number;
  y: number;
  radius: number;
  color: string;
  facing?: 'down' | 'up' | 'left' | 'right';
  patrol?: NpcWaypoint[];
  patrolSpeed?: number;
  dialogues: DialogueLine[];
  clues: ClueDef[];
}

export interface PlayObjectDef {
  id: string;
  label: string;
  x: number;
  y: number;
  radius: number;
  icon: string;
  inspectTitle: string;
  inspectLines: DialogueLine[];
  clues: ClueDef[];
}

export interface SceneZone {
  id: string;
  label: string;
  x: number;
  y: number;
  w: number;
  h: number;
  floor: string;
  accent?: string;
  /** Nombre del SVG en assets/simulacion/{scene}/zones/ */
  tileId: string;
}

export interface SceneDoor {
  x: number;
  y: number;
  w: number;
  h: number;
  orientation: 'horizontal' | 'vertical';
}

export type FurnitureKind =
  | 'desk'
  | 'chair'
  | 'bed'
  | 'cabinet'
  | 'counter'
  | 'sign'
  | 'divider';

export interface SceneFurniture {
  kind: FurnitureKind;
  x: number;
  y: number;
  w: number;
  h: number;
  color?: string;
  label?: string;
}

export interface PlaySceneConfig {
  id: SceneId;
  title: string;
  subtitle: string;
  worldWidth: number;
  worldHeight: number;
  playerStart: { x: number; y: number };
  bounds: { x: number; y: number; w: number; h: number };
  interactDistance: number;
  zones: SceneZone[];
  doors?: SceneDoor[];
  furniture: SceneFurniture[];
  npcs: PlayNpcDef[];
  objects: PlayObjectDef[];
  obstacles: { x: number; y: number; w: number; h: number }[];
}

export const EXPLORATION_GATING = {
  minNpcs: 2,
  minObjects: 2,
} as const;
