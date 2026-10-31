import { ImmersiveRoomDef } from './immersive-room.types';

export type ComisariaRoomId =
  | 'recepcion'
  | 'sala-espera'
  | 'oficina-atencion'
  | 'area-psicosocial'
  | 'despacho';

export const COMISARIA_ROOMS: ImmersiveRoomDef[] = [
  {
    id: 'recepcion',
    label: 'Recepción',
    ambiance: 'Ingreso institucional · Comisaría de Familia',
    background: '/assets/simulacion/comisaria/rooms/recepcion.svg',
    npcIds: ['secretaria'],
    objectIds: [],
    exits: ['sala-espera', 'oficina-atencion'],
    hotspots: [
      { x: 48, y: 44, w: 16, h: 34, npcId: 'secretaria' },
      { x: 20, y: 30, w: 14, h: 14, observeId: 'obs-com-recepcion-escudo' },
      { x: 72, y: 48, w: 16, h: 28, exitTo: 'sala-espera', exitLabel: 'Sala de espera' },
      { x: 42, y: 68, w: 18, h: 20, exitTo: 'oficina-atencion', exitLabel: 'Oficina de atención' },
    ],
  },
  {
    id: 'sala-espera',
    label: 'Sala de espera',
    ambiance: 'Acompañantes y víctima en espera',
    background: '/assets/simulacion/comisaria/rooms/sala-espera.svg',
    npcIds: ['victima'],
    objectIds: [],
    exits: ['recepcion', 'area-psicosocial'],
    hotspots: [
      { x: 42, y: 46, w: 16, h: 32, npcId: 'victima' },
      { x: 68, y: 40, w: 12, h: 14, observeId: 'obs-com-sala-avisos' },
      { x: 8, y: 48, w: 12, h: 28, exitTo: 'recepcion', exitLabel: 'Recepción' },
      { x: 82, y: 44, w: 14, h: 32, exitTo: 'area-psicosocial', exitLabel: 'Área psicosocial' },
    ],
  },
  {
    id: 'oficina-atencion',
    label: 'Oficina de atención',
    ambiance: 'Ventanilla y trámites',
    background: '/assets/simulacion/comisaria/rooms/oficina-atencion.svg',
    npcIds: [],
    objectIds: [],
    exits: ['recepcion', 'despacho'],
    hotspots: [
      { x: 32, y: 36, w: 24, h: 18, observeId: 'obs-com-oficina-tramites' },
      { x: 58, y: 42, w: 14, h: 16, observeId: 'obs-com-oficina-radicados' },
      { x: 8, y: 50, w: 12, h: 28, exitTo: 'recepcion', exitLabel: 'Recepción' },
      { x: 78, y: 46, w: 16, h: 30, exitTo: 'despacho', exitLabel: 'Despacho del comisario' },
    ],
  },
  {
    id: 'area-psicosocial',
    label: 'Área psicosocial',
    ambiance: 'Trabajo social y protección',
    background: '/assets/simulacion/comisaria/rooms/area-psicosocial.svg',
    npcIds: ['trabajador-social', 'proteccion'],
    objectIds: [],
    exits: ['sala-espera', 'despacho'],
    hotspots: [
      { x: 22, y: 44, w: 16, h: 34, npcId: 'trabajador-social' },
      { x: 58, y: 44, w: 16, h: 34, npcId: 'proteccion' },
      { x: 40, y: 28, w: 18, h: 14, observeId: 'obs-com-psico-ruta' },
      { x: 6, y: 48, w: 12, h: 28, exitTo: 'sala-espera', exitLabel: 'Sala de espera' },
      { x: 84, y: 44, w: 14, h: 32, exitTo: 'despacho', exitLabel: 'Despacho' },
    ],
  },
  {
    id: 'despacho',
    label: 'Despacho del comisario',
    ambiance: 'Decisión institucional · Archivo del caso',
    background: '/assets/simulacion/comisaria/rooms/despacho.svg',
    npcIds: ['comisario'],
    objectIds: ['expediente', 'denuncias', 'reportes', 'ruta-proteccion'],
    exits: ['oficina-atencion', 'area-psicosocial'],
    hotspots: [
      { x: 38, y: 42, w: 16, h: 32, npcId: 'comisario' },
      { x: 62, y: 38, w: 14, h: 24, objectId: 'expediente' },
      { x: 78, y: 40, w: 12, h: 22, objectId: 'denuncias' },
      { x: 62, y: 62, w: 12, h: 20, objectId: 'reportes' },
      { x: 78, y: 62, w: 12, h: 20, objectId: 'ruta-proteccion' },
      { x: 8, y: 50, w: 12, h: 28, exitTo: 'oficina-atencion', exitLabel: 'Oficina de atención' },
      { x: 18, y: 48, w: 12, h: 26, exitTo: 'area-psicosocial', exitLabel: 'Área psicosocial' },
    ],
  },
];

export const COMISARIA_ROOM_MAP = new Map(COMISARIA_ROOMS.map((r) => [r.id, r]));

export const DEFAULT_COMISARIA_ROOM: ComisariaRoomId = 'recepcion';
