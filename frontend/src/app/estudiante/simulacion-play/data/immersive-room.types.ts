/** Tipos compartidos para escenas inmersivas (hospital y comisaría). */
export interface RoomHotspot {
  x: number;
  y: number;
  w: number;
  h: number;
  npcId?: string;
  objectId?: string;
  observeId?: string;
  exitTo?: string;
  exitLabel?: string;
}

export interface ImmersiveRoomDef {
  id: string;
  label: string;
  ambiance: string;
  background: string;
  npcIds: string[];
  objectIds: string[];
  exits: string[];
  hotspots: RoomHotspot[];
}
