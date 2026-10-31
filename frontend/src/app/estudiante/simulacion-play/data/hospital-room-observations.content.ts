import { HospitalRoomId } from './hospital-rooms.content';

export interface RoomObservationDef {
  id: string;
  roomId: HospitalRoomId;
  label: string;
  text: string;
  /** Pista opcional de contexto (no desbloquea tier académico). */
  flavorClueId?: string;
}

/** Elementos del entorno observables (sin sustituir entrevistas ni documentos). */
export const HOSPITAL_OBSERVATIONS: RoomObservationDef[] = [
  {
    id: 'obs-entrada-urgencias',
    roomId: 'entrada',
    label: 'Letrero de urgencias',
    text: 'El servicio de urgencias opera a plena capacidad. Hay señalización de ingreso restringido para acompañantes.',
  },
  {
    id: 'obs-entrada-ambulancia',
    roomId: 'entrada',
    label: 'Vehículo de emergencia',
    text: 'Una ambulancia acaba de estacionarse. El personal transfiere a una paciente con aparente lesión en miembro superior.',
  },
  {
    id: 'obs-recepcion-mostrador',
    roomId: 'recepcion',
    label: 'Mostrador de admisión',
    text: 'En el mostrador hay formularios de ingreso y un aviso de protocolo de violencia intrafamiliar en instituciones de salud.',
  },
  {
    id: 'obs-recepcion-carteles',
    roomId: 'recepcion',
    label: 'Cartelería institucional',
    text: 'Carteles indican derivación a trabajo social y línea de atención psicológica para situaciones de crisis.',
  },
  {
    id: 'obs-sala-revistas',
    roomId: 'sala-espera',
    label: 'Revistero y silencio',
    text: 'La sala está en silencio tenso. Una familia espera sin apartarse del mostrador de admisión.',
  },
  {
    id: 'obs-sala-tv',
    roomId: 'sala-espera',
    label: 'Televisor',
    text: 'El televisor transmite noticias locales; nadie presta atención. Hay restos de café en la mesa central.',
  },
  {
    id: 'obs-pasillo-senales',
    roomId: 'pasillo',
    label: 'Señalización',
    text: 'Pasillo hacia UCI, enfermería y archivo clínico. Personal sanitario transita con prisa.',
  },
  {
    id: 'obs-pasillo-camilla',
    roomId: 'pasillo',
    label: 'Camilla en tránsito',
    text: 'Una camilla acaba de pasar hacia el área médica. Escuchas fragmentos de conversación sobre “estabilización”.',
  },
  {
    id: 'obs-medica-monitores',
    roomId: 'area-medica',
    label: 'Monitores de signos vitales',
    text: 'Los monitores muestran ritmo estable. Hay notas de enfermería sobre ingreso por lesión y antecedente de conflicto doméstico.',
  },
  {
    id: 'obs-medica-medicacion',
    roomId: 'area-medica',
    label: 'Carro de medicación',
    text: 'Medicación de urgencia preparada. El protocolo indica valoración integral, no solo atención de la lesión.',
  },
  {
    id: 'obs-consultorio-archivo',
    roomId: 'consultorio',
    label: 'Archivador clínico',
    text: 'Carpetas etiquetadas por episodio. Algunos documentos están marcados como “confidencial — valoración psicosocial”.',
  },
  {
    id: 'obs-consultorio-escritorio',
    roomId: 'consultorio',
    label: 'Escritorio de entrevista',
    text: 'Espacio reservado para entrevista privada. Hay formularios de valoración de riesgo y consentimiento informado.',
  },
];

export const OBSERVATIONS_BY_ROOM = new Map<HospitalRoomId, RoomObservationDef[]>();
for (const o of HOSPITAL_OBSERVATIONS) {
  const list = OBSERVATIONS_BY_ROOM.get(o.roomId) ?? [];
  list.push(o);
  OBSERVATIONS_BY_ROOM.set(o.roomId, list);
}
