import { ContradictionDef } from './investigation.types';

export const CONTRADICTIONS_CATALOG: ContradictionDef[] = [
  {
    id: 'con-madre-hermano',
    sceneId: 'hospital',
    title: 'Madre vs Hermano',
    clueIdA: 'h-madre-t2',
    clueIdB: 'h-hermano-t2',
    narrative: 'La versión de la madre no coincide con la del hermano sobre si hubo agresiones previas.',
  },
  {
    id: 'con-madre-historia',
    sceneId: 'hospital',
    title: 'Madre vs Historia clínica',
    clueIdA: 'h-madre-t2',
    clueIdB: 'h-historia-t2',
    narrative: 'El relato de “incidente aislado” choca con antecedentes sugeridos en el registro médico.',
  },
  {
    id: 'con-victima-expediente',
    sceneId: 'comisaria',
    title: 'Víctima vs Expediente',
    clueIdA: 'c-victima-t2',
    clueIdB: 'c-expediente-t2',
    narrative: 'La víctima niega denuncia previa, pero el expediente registra una radicación anterior.',
  },
  {
    id: 'con-victima-denuncias',
    sceneId: 'comisaria',
    title: 'Víctima vs Archivo de denuncias',
    clueIdA: 'c-victima-t2',
    clueIdB: 'c-denuncias-t1',
    narrative: 'El testimonio oral no coincide con la constancia en archivo de denuncias.',
  },
];
