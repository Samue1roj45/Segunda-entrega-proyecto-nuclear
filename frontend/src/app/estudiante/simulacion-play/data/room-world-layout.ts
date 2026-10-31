/** Layout de exploración (solo presentación; no altera hotspots académicos). */
export const WORLD_WIDTH = 3200;
export const WORLD_HEIGHT = 1920;
export const PLAYER_RADIUS = 22;
export const MOVE_SPEED_PX = 240;
export const INTERACT_RADIUS_PX = 130;
export const CAMERA_LERP = 0.078;

export interface WorldRectPct {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface RoomWorldLayout {
  spawn: { x: number; y: number };
  obstacles: WorldRectPct[];
}

export interface MapGraphNode {
  id: string;
  label: string;
  x: number;
  y: number;
}

export interface InstitutionMapGraph {
  nodes: MapGraphNode[];
  edges: [string, string][];
}

const wall = (x: number, y: number, w: number, h: number): WorldRectPct => ({ x, y, w, h });

const HOSPITAL_LAYOUTS: Record<string, RoomWorldLayout> = {
  entrada: {
    spawn: { x: 48, y: 72 },
    obstacles: [
      wall(0, 0, 100, 12),
      wall(0, 0, 6, 100),
      wall(94, 0, 6, 100),
      wall(8, 48, 18, 22),
      wall(48, 18, 28, 12),
    ],
  },
  recepcion: {
    spawn: { x: 50, y: 75 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(22, 32, 28, 22),
      wall(55, 55, 35, 18),
    ],
  },
  'sala-espera': {
    spawn: { x: 50, y: 78 },
    obstacles: [
      wall(0, 0, 100, 12),
      wall(0, 0, 4, 100),
      wall(96, 0, 4, 100),
      wall(8, 52, 12, 20),
      wall(22, 52, 12, 20),
      wall(36, 52, 12, 20),
      wall(50, 52, 12, 20),
      wall(82, 22, 20, 28),
    ],
  },
  pasillo: {
    spawn: { x: 50, y: 70 },
    obstacles: [
      wall(0, 0, 100, 8),
      wall(0, 0, 8, 100),
      wall(92, 0, 8, 100),
      wall(38, 42, 22, 14),
      wall(55, 38, 8, 28),
    ],
  },
  'area-medica': {
    spawn: { x: 50, y: 72 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(8, 48, 14, 18),
      wall(68, 22, 18, 20),
      wall(24, 52, 20, 16),
    ],
  },
  consultorio: {
    spawn: { x: 50, y: 74 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(18, 32, 22, 18),
      wall(52, 30, 20, 20),
      wall(68, 42, 22, 22),
    ],
  },
};

const COMISARIA_LAYOUTS: Record<string, RoomWorldLayout> = {
  recepcion: {
    spawn: { x: 50, y: 72 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(18, 28, 24, 20),
      wall(48, 58, 30, 16),
    ],
  },
  'sala-espera': {
    spawn: { x: 50, y: 76 },
    obstacles: [
      wall(0, 0, 100, 12),
      wall(0, 0, 4, 100),
      wall(96, 0, 4, 100),
      wall(10, 50, 10, 18),
      wall(26, 50, 10, 18),
      wall(62, 32, 18, 22),
    ],
  },
  'oficina-atencion': {
    spawn: { x: 50, y: 70 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(26, 30, 32, 22),
      wall(58, 36, 28, 18),
    ],
  },
  'area-psicosocial': {
    spawn: { x: 50, y: 72 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(12, 38, 18, 24),
      wall(52, 38, 18, 24),
      wall(36, 22, 28, 12),
    ],
  },
  despacho: {
    spawn: { x: 50, y: 74 },
    obstacles: [
      wall(0, 0, 100, 10),
      wall(0, 0, 5, 100),
      wall(95, 0, 5, 100),
      wall(30, 36, 22, 20),
      wall(58, 34, 28, 28),
      wall(72, 52, 14, 18),
    ],
  },
};

export function roomLayout(institution: 'hospital' | 'comisaria', roomId: string): RoomWorldLayout {
  const table = institution === 'hospital' ? HOSPITAL_LAYOUTS : COMISARIA_LAYOUTS;
  return (
    table[roomId] ?? {
      spawn: { x: 48, y: 72 },
      obstacles: [wall(0, 0, 100, 8), wall(0, 0, 4, 100), wall(96, 0, 4, 100)],
    }
  );
}

export function pctToPxX(pct: number): number {
  return (pct / 100) * WORLD_WIDTH;
}

export function pctToPxY(pct: number): number {
  return (pct / 100) * WORLD_HEIGHT;
}

export function obstaclesToPx(layout: RoomWorldLayout): { x: number; y: number; w: number; h: number }[] {
  return layout.obstacles.map((o) => ({
    x: pctToPxX(o.x),
    y: pctToPxY(o.y),
    w: pctToPxX(o.w),
    h: pctToPxY(o.h),
  }));
}

export const HOSPITAL_MAP_GRAPH: InstitutionMapGraph = {
  nodes: [
    { id: 'entrada', label: 'Entrada', x: 50, y: 88 },
    { id: 'recepcion', label: 'Recepción', x: 50, y: 58 },
    { id: 'sala-espera', label: 'Sala espera', x: 78, y: 58 },
    { id: 'pasillo', label: 'Pasillo', x: 28, y: 42 },
    { id: 'area-medica', label: 'Área médica', x: 50, y: 22 },
    { id: 'consultorio', label: 'Consultorio', x: 78, y: 22 },
  ],
  edges: [
    ['entrada', 'recepcion'],
    ['entrada', 'pasillo'],
    ['recepcion', 'sala-espera'],
    ['recepcion', 'pasillo'],
    ['sala-espera', 'pasillo'],
    ['pasillo', 'area-medica'],
    ['pasillo', 'consultorio'],
    ['area-medica', 'consultorio'],
  ],
};

export const COMISARIA_MAP_GRAPH: InstitutionMapGraph = {
  nodes: [
    { id: 'recepcion', label: 'Recepción', x: 50, y: 82 },
    { id: 'sala-espera', label: 'Sala espera', x: 78, y: 58 },
    { id: 'oficina-atencion', label: 'Oficina', x: 50, y: 42 },
    { id: 'area-psicosocial', label: 'Psicosocial', x: 78, y: 28 },
    { id: 'despacho', label: 'Despacho', x: 28, y: 28 },
  ],
  edges: [
    ['recepcion', 'sala-espera'],
    ['recepcion', 'oficina-atencion'],
    ['sala-espera', 'area-psicosocial'],
    ['oficina-atencion', 'despacho'],
    ['area-psicosocial', 'despacho'],
  ],
};
