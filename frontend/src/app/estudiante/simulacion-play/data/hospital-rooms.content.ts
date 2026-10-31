/** Espacios hospitalarios independientes (navegación inmersiva; mismos NPC/objetos académicos). */
export type HospitalRoomId =
  | 'entrada'
  | 'recepcion'
  | 'sala-espera'
  | 'pasillo'
  | 'area-medica'
  | 'consultorio';

export interface RoomHotspot {
  x: number;
  y: number;
  w: number;
  h: number;
  npcId?: string;
  objectId?: string;
  observeId?: string;
  exitTo?: HospitalRoomId;
  exitLabel?: string;
  /** Ancla visual del personaje al suelo (centro inferior del hotspot). */
  anchor?: 'floor-left' | 'floor-center' | 'floor-right';
}

export interface HospitalRoomDef {
  id: HospitalRoomId;
  label: string;
  ambiance: string;
  background: string;
  npcIds: string[];
  objectIds: string[];
  exits: HospitalRoomId[];
  hotspots: RoomHotspot[];
}

export const HOSPITAL_ROOMS: HospitalRoomDef[] = [
  {
    id: 'entrada',
    label: 'Entrada principal',
    ambiance: 'Fachada nocturna · Urgencias',
    background: '/assets/simulacion/hospital/rooms/entrada.svg',
    npcIds: ['guardia'],
    objectIds: [],
    exits: ['recepcion', 'pasillo'],
    hotspots: [
      { x: 52, y: 42, w: 16, h: 32, npcId: 'guardia', anchor: 'floor-center' },
      { x: 38, y: 28, w: 10, h: 14, observeId: 'obs-entrada-urgencias' },
      { x: 12, y: 52, w: 14, h: 18, observeId: 'obs-entrada-ambulancia' },
      { x: 72, y: 52, w: 18, h: 28, exitTo: 'recepcion', exitLabel: 'Recepción' },
      { x: 8, y: 42, w: 14, h: 32, exitTo: 'pasillo', exitLabel: 'Pasillo de urgencias' },
    ],
  },
  {
    id: 'recepcion',
    label: 'Recepción',
    ambiance: 'Admisión y señalética clínica',
    background: '/assets/simulacion/hospital/rooms/recepcion.svg',
    npcIds: [],
    objectIds: [],
    exits: ['entrada', 'sala-espera', 'pasillo'],
    hotspots: [
      { x: 28, y: 38, w: 22, h: 20, observeId: 'obs-recepcion-mostrador' },
      { x: 62, y: 22, w: 18, h: 16, observeId: 'obs-recepcion-carteles' },
      { x: 6, y: 55, w: 12, h: 28, exitTo: 'entrada', exitLabel: 'Salida' },
      { x: 78, y: 48, w: 16, h: 30, exitTo: 'sala-espera', exitLabel: 'Sala de espera' },
      { x: 42, y: 72, w: 20, h: 18, exitTo: 'pasillo', exitLabel: 'Pasillo' },
    ],
  },
  {
    id: 'sala-espera',
    label: 'Sala de espera',
    ambiance: 'Acompañantes · Zona de espera',
    background: '/assets/simulacion/hospital/rooms/sala-espera.svg',
    npcIds: ['madre', 'hermano'],
    objectIds: [],
    exits: ['recepcion', 'pasillo'],
    hotspots: [
      { x: 18, y: 44, w: 16, h: 34, npcId: 'madre', anchor: 'floor-center' },
      { x: 64, y: 46, w: 16, h: 34, npcId: 'hermano', anchor: 'floor-center' },
      { x: 48, y: 32, w: 12, h: 14, observeId: 'obs-sala-revistas' },
      { x: 78, y: 28, w: 12, h: 14, observeId: 'obs-sala-tv' },
      { x: 4, y: 50, w: 12, h: 28, exitTo: 'recepcion', exitLabel: 'Recepción' },
      { x: 88, y: 45, w: 14, h: 32, exitTo: 'pasillo', exitLabel: 'Pasillo' },
    ],
  },
  {
    id: 'pasillo',
    label: 'Pasillo de urgencias',
    ambiance: 'Circulación · Señalización médica',
    background: '/assets/simulacion/hospital/rooms/pasillo.svg',
    npcIds: [],
    objectIds: [],
    exits: ['entrada', 'recepcion', 'sala-espera', 'area-medica', 'consultorio'],
    hotspots: [
      { x: 40, y: 30, w: 20, h: 16, observeId: 'obs-pasillo-senales' },
      { x: 58, y: 48, w: 16, h: 18, observeId: 'obs-pasillo-camilla' },
      { x: 4, y: 40, w: 12, h: 35, exitTo: 'entrada', exitLabel: 'Entrada' },
      { x: 18, y: 38, w: 14, h: 32, exitTo: 'recepcion', exitLabel: 'Recepción' },
      { x: 32, y: 38, w: 14, h: 32, exitTo: 'sala-espera', exitLabel: 'Sala de espera' },
      { x: 52, y: 35, w: 16, h: 38, exitTo: 'area-medica', exitLabel: 'Área médica' },
      { x: 78, y: 35, w: 18, h: 38, exitTo: 'consultorio', exitLabel: 'Consultorio' },
    ],
  },
  {
    id: 'area-medica',
    label: 'Área médica',
    ambiance: 'Enfermería · Monitoreo · Urgencias',
    background: '/assets/simulacion/hospital/rooms/area-medica.svg',
    npcIds: ['enfermeria', 'medico'],
    objectIds: [],
    exits: ['pasillo', 'consultorio'],
    hotspots: [
      { x: 14, y: 40, w: 18, h: 36, npcId: 'enfermeria', anchor: 'floor-center' },
      { x: 50, y: 38, w: 18, h: 38, npcId: 'medico', anchor: 'floor-center' },
      { x: 72, y: 28, w: 14, h: 16, observeId: 'obs-medica-monitores' },
      { x: 28, y: 58, w: 12, h: 14, observeId: 'obs-medica-medicacion' },
      { x: 6, y: 48, w: 12, h: 30, exitTo: 'pasillo', exitLabel: 'Pasillo' },
      { x: 82, y: 46, w: 14, h: 28, exitTo: 'consultorio', exitLabel: 'Consultorio' },
    ],
  },
  {
    id: 'consultorio',
    label: 'Consultorio de entrevista',
    ambiance: 'Documentación · Archivo clínico',
    background: '/assets/simulacion/hospital/rooms/consultorio.svg',
    npcIds: [],
    objectIds: ['historia-clinica', 'protocolo-epicee'],
    exits: ['pasillo', 'area-medica'],
    hotspots: [
      { x: 24, y: 38, w: 16, h: 26, objectId: 'historia-clinica' },
      { x: 58, y: 36, w: 16, h: 28, objectId: 'protocolo-epicee' },
      { x: 40, y: 22, w: 16, h: 14, observeId: 'obs-consultorio-archivo' },
      { x: 68, y: 52, w: 18, h: 16, observeId: 'obs-consultorio-escritorio' },
      { x: 6, y: 50, w: 12, h: 28, exitTo: 'pasillo', exitLabel: 'Pasillo' },
      { x: 18, y: 48, w: 12, h: 26, exitTo: 'area-medica', exitLabel: 'Área médica' },
    ],
  },
];

export const HOSPITAL_ROOM_MAP = new Map(HOSPITAL_ROOMS.map((r) => [r.id, r]));

export const DEFAULT_HOSPITAL_ROOM: HospitalRoomId = 'entrada';
