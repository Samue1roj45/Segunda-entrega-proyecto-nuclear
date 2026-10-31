import { PersonaDef } from './investigation.types';

export const PERSONAS_CATALOG: PersonaDef[] = [
  { id: 'medico', sceneId: 'hospital', nombre: 'Médico de urgencias', rol: 'Equipo médico', color: '#0ea5e9', shortLabel: 'Médico' },
  { id: 'madre', sceneId: 'hospital', nombre: 'Madre de la paciente', rol: 'Familia', color: '#a855f7', shortLabel: 'Madre' },
  { id: 'hermano', sceneId: 'hospital', nombre: 'Hermano de la paciente', rol: 'Familia', color: '#6366f1', shortLabel: 'Hermano' },
  { id: 'enfermeria', sceneId: 'hospital', nombre: 'Enfermería', rol: 'Equipo de enfermería', color: '#f472b6', shortLabel: 'Enfermería' },
  { id: 'guardia', sceneId: 'hospital', nombre: 'Guardia de seguridad', rol: 'Seguridad', color: '#64748b', shortLabel: 'Guardia' },
  { id: 'victima', sceneId: 'comisaria', nombre: 'Víctima', rol: 'Superviviente', color: '#ec4899', shortLabel: 'Víctima' },
  { id: 'comisario', sceneId: 'comisaria', nombre: 'Comisario de familia', rol: 'Institución', color: '#2563eb', shortLabel: 'Comisario' },
  { id: 'trabajador-social', sceneId: 'comisaria', nombre: 'Trabajador social', rol: 'Trabajo social', color: '#14b8a6', shortLabel: 'Trab. Social' },
  { id: 'secretaria', sceneId: 'comisaria', nombre: 'Secretaría', rol: 'Administración', color: '#78716c', shortLabel: 'Secretaría' },
  { id: 'proteccion', sceneId: 'comisaria', nombre: 'Equipo de protección', rol: 'Protección', color: '#ca8a04', shortLabel: 'Protección' },
];
