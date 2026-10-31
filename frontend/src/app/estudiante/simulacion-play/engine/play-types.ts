export interface Vec2 {
  x: number;
  y: number;
}

export interface Rect {
  x: number;
  y: number;
  w: number;
  h: number;
}

export type InteractableKind = 'npc' | 'object';

export interface InteractableTarget {
  kind: InteractableKind;
  id: string;
  label: string;
  distance: number;
}
