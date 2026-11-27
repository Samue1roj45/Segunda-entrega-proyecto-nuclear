package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;

import java.util.List;

/**
 * Administración de cuentas: consultar activas y archivadas, archivar, reactivar y eliminar cuando es
 * seguro. Cada operación declara el permiso administrativo existente; el Proxy de acceso lo verifica.
 */
public interface ServicioAdministracionUsuarios {

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    List<UsuarioAdminDTO> listar(boolean activos, Rol rol);

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    UsuarioAdminDTO archivar(Long idUsuario);

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    UsuarioAdminDTO reactivar(Long idUsuario);

    /** Informa si la cuenta se puede eliminar y, si no, por qué. No cambia nada. */
    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    EvaluacionEliminacionDTO evaluarEliminacion(Long idUsuario);

    /** Borra la cuenta solo si la cadena de eliminación lo permite; si no, responde 409 con el motivo. */
    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    void eliminar(Long idUsuario);
}
