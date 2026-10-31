import { ComisariaRoomId } from './comisaria-rooms.content';

export interface ComisariaObservationDef {
  id: string;
  roomId: ComisariaRoomId;
  label: string;
  text: string;
}

export const COMISARIA_OBSERVATIONS: ComisariaObservationDef[] = [
  {
    id: 'obs-com-recepcion-escudo',
    roomId: 'recepcion',
    label: 'Escudo institucional',
    text: 'Comisaría de Familia. Aviso de atención a víctimas de violencia intrafamiliar y protocolo de confidencialidad.',
  },
  {
    id: 'obs-com-sala-avisos',
    roomId: 'sala-espera',
    label: 'Carteleras de apoyo',
    text: 'Folletos sobre líneas de ayuda, rutas de protección y derechos de las mujeres en situación de riesgo.',
  },
  {
    id: 'obs-com-oficina-tramites',
    roomId: 'oficina-atencion',
    label: 'Ventanilla de trámites',
    text: 'Formularios de radicación, constancias de denuncia y solicitudes de medidas de protección.',
  },
  {
    id: 'obs-com-oficina-radicados',
    roomId: 'oficina-atencion',
    label: 'Registro de radicados',
    text: 'Lista de casos activos del turno. El caso actual aparece marcado como prioritario.',
  },
  {
    id: 'obs-com-psico-ruta',
    roomId: 'area-psicosocial',
    label: 'Diagrama de ruta',
    text: 'Esquema interinstitucional: hospital, comisaría, trabajo social, fiscalía y red de apoyo.',
  },
];

export const COMISARIA_OBS_MAP = new Map(COMISARIA_OBSERVATIONS.map((o) => [o.id, o]));
