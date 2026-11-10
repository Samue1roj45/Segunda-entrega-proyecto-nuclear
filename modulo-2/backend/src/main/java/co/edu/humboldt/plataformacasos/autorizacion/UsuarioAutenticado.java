package co.edu.humboldt.plataformacasos.autorizacion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;

import java.util.Set;

/** Identidad verificada del usuario que hace la petición, con sus permisos vigentes en la base. */
public record UsuarioAutenticado(Long id, String correoInstitucional, String nombreCompleto, Rol rol,
                                 Set<Permiso> permisos) {

    public boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }
}
