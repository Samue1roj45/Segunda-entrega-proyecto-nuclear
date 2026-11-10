import { Permiso } from './modelos/permiso';

/** Opción del menú principal. Se muestra solo si la sesión tiene su permiso (RF-02). */
export interface OpcionMenu {
  readonly permiso: Permiso;
  readonly titulo: string;
  readonly descripcion: string;
  /** Ruta de la pantalla; null mientras el módulo que la implementa no esté integrado. */
  readonly ruta: string | null;
  readonly modulo: number;
}

export const OPCIONES_MENU: readonly OpcionMenu[] = [
  { permiso: Permiso.ParticiparEnCasos, titulo: 'Participar en casos', descripcion: 'Resolver los casos de las sesiones a las que fue convocado.', ruta: null, modulo: 4 },
  { permiso: Permiso.ConsultarResultadosPropios, titulo: 'Mis resultados', descripcion: 'Consultar la calificación final de sus participaciones.', ruta: null, modulo: 5 },
  { permiso: Permiso.GestionarCasos, titulo: 'Casos de estudio', descripcion: 'Crear, editar y publicar casos y rúbricas.', ruta: '/docente/casos', modulo: 2 },
  { permiso: Permiso.ProgramarSesiones, titulo: 'Programar sesiones', descripcion: 'Asociar estudiantes y programar convocatorias.', ruta: null, modulo: 3 },
  { permiso: Permiso.ConsultarParticipaciones, titulo: 'Participaciones', descripcion: 'Consultar las participaciones de los estudiantes.', ruta: null, modulo: 5 },
  { permiso: Permiso.DescargarReportes, titulo: 'Reportes', descripcion: 'Descargar los reportes de evaluación.', ruta: null, modulo: 5 },
  { permiso: Permiso.RegistrarRetroalimentacion, titulo: 'Retroalimentación', descripcion: 'Registrar la valoración docente.', ruta: null, modulo: 6 },
  { permiso: Permiso.AutorizarRepeticion, titulo: 'Repeticiones', descripcion: 'Autorizar la repetición excepcional de un caso.', ruta: null, modulo: 4 },
  { permiso: Permiso.AdministrarDocentes, titulo: 'Docentes y permisos', descripcion: 'Crear docentes y asignarles o retirarles permisos.', ruta: null, modulo: 1 },
];
