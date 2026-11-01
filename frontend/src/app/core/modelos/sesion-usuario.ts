import { Permiso } from './permiso';
import { Rol } from './rol';

/**
 * Datos de la sesión que devuelve el backend al autenticar.
 * El frontend los usa solo para mostrar u ocultar opciones;
 * la autorización real la hace el servidor (RF-02).
 */
export interface SesionUsuario {
  readonly token: string;
  readonly correoInstitucional: string;
  readonly nombreCompleto: string;
  readonly rol: Rol;
  readonly permisos: readonly Permiso[];
}
