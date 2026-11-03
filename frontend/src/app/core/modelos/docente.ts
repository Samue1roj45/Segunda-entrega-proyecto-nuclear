import { Permiso } from './permiso';

/** Docente tal como lo devuelve el API de administración (DocenteDTO del backend). */
export interface Docente {
  readonly id: number;
  readonly correoInstitucional: string;
  readonly nombreCompleto: string;
  readonly activo: boolean;
  readonly permisos: readonly Permiso[];
}

/** Cuerpo para crear un docente (CrearDocenteRequest del backend). */
export interface CrearDocente {
  readonly nombreCompleto: string;
  readonly correoInstitucional: string;
}

/** Descripción de cada permiso, igual a la del enum Permiso del backend. */
export const DESCRIPCION_PERMISO: Readonly<Record<Permiso, string>> = {
  [Permiso.ParticiparEnCasos]: 'Participar en casos y usar sus herramientas',
  [Permiso.ConsultarResultadosPropios]: 'Consultar el resultado final propio',
  [Permiso.GestionarCasos]: 'Crear, editar y publicar casos y rúbricas',
  [Permiso.ProgramarSesiones]: 'Asociar estudiantes y programar convocatorias',
  [Permiso.ConsultarParticipaciones]: 'Consultar participaciones',
  [Permiso.DescargarReportes]: 'Descargar reportes',
  [Permiso.RegistrarRetroalimentacion]: 'Registrar valoración docente',
  [Permiso.AutorizarRepeticion]: 'Autorizar repetición excepcional',
  [Permiso.AdministrarDocentes]: 'Crear docentes y asignar permisos',
};

/** Permisos que el administrador puede asignar a un docente (los marcados como asignables en el backend). */
export const PERMISOS_ASIGNABLES: readonly Permiso[] = [
  Permiso.GestionarCasos,
  Permiso.ProgramarSesiones,
  Permiso.ConsultarParticipaciones,
  Permiso.DescargarReportes,
  Permiso.RegistrarRetroalimentacion,
  Permiso.AutorizarRepeticion,
];
