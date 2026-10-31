import { HechoCandidatoDef } from './investigation.types';

export const HECHOS_CANDIDATOS: HechoCandidatoDef[] = [
  {
    id: 'f-violencia-previa',
    sceneId: 'global',
    label: 'Existió violencia previa en el hogar',
    unlockClueIds: ['h-hermano-t2', 'h-historia-t2', 'c-expediente-t2'],
  },
  {
    id: 'f-menor-presente',
    sceneId: 'global',
    label: 'Había una menor presente en la situación',
    unlockClueIds: ['h-enfermeria-t2', 'h-epicee-t2', 'c-victima-t3', 'c-tsocial-t2'],
  },
  {
    id: 'f-riesgo-repeticion',
    sceneId: 'global',
    label: 'Existía riesgo de repetición de la violencia',
    unlockClueIds: ['h-medico-t2', 'c-proteccion-t2'],
  },
  {
    id: 'f-ruta-proteccion',
    sceneId: 'global',
    label: 'Era necesaria la activación de una ruta de protección',
    unlockClueIds: ['h-epicee-t1', 'c-comisario-t2', 'c-ruta-t1', 'c-denuncias-t2'],
  },
  {
    id: 'f-conflicto-familiar',
    sceneId: 'hospital',
    label: 'El conflicto involucraba dinámicas familiares complejas',
    unlockClueIds: ['h-madre-t1', 'h-hermano-t1'],
  },
];
