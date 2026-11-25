package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;

import java.time.LocalDateTime;

/** Vista de una cuenta para la administración de usuarios. No expone hashes ni permisos internos. */
public record UsuarioAdminDTO(Long id, String correoInstitucional, String nombreCompleto, Rol rol, boolean activo,
                              LocalDateTime fechaCreacion) {

    public static UsuarioAdminDTO de(Usuario usuario) {
        return new UsuarioAdminDTO(usuario.getId(), usuario.getCorreoInstitucional(), usuario.getNombreCompleto(),
                usuario.getRol(), usuario.isActivo(), usuario.getFechaCreacion());
    }
}
