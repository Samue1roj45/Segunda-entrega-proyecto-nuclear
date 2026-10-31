import { EvidenciaDef } from './investigation.types';

export const EVIDENCIAS_CATALOG: EvidenciaDef[] = [
  {
    id: 'historia-clinica',
    sceneId: 'hospital',
    titulo: 'Historia clínica',
    descripcion: 'Registro de ingreso y notas preliminares de urgencias.',
  },
  {
    id: 'protocolo-epicee',
    sceneId: 'hospital',
    titulo: 'Protocolo EPICEE',
    descripcion: 'Marco de evaluación de riesgo y coordinación interinstitucional.',
  },
  {
    id: 'expediente',
    sceneId: 'comisaria',
    titulo: 'Expediente del caso',
    descripcion: 'Archivo institucional con antecedentes y radicados.',
  },
  {
    id: 'ruta-proteccion',
    sceneId: 'comisaria',
    titulo: 'Ruta de protección',
    descripcion: 'Diagrama de la cadena de atención y medidas.',
  },
  {
    id: 'reportes',
    sceneId: 'comisaria',
    titulo: 'Reportes interinstitucionales',
    descripcion: 'Informes de hospital y trabajo social.',
  },
  {
    id: 'denuncias',
    sceneId: 'comisaria',
    titulo: 'Archivo de denuncias',
    descripcion: 'Constancias y radicados de denuncias previas.',
  },
];
