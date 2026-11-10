package co.edu.humboldt.plataformacasos.autorizacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.NoAutenticadoException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-02: decide si el usuario actual puede usar una función. Los permisos se leen de la base
 * en cada verificación (no del token), así un permiso retirado o un usuario desactivado
 * dejan de tener acceso de inmediato.
 */
@Service
public class ServicioAutorizacion {

    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioAutorizacion(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    /** Identidad del usuario de la petición actual; 401 si no hay sesión válida o el usuario está inactivo. */
    @Transactional(readOnly = true)
    public UsuarioAutenticado verificarIdentidad() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken token)) {
            throw new NoAutenticadoException();
        }
        Usuario usuario = idDe(token)
                .flatMap(usuarioRepositorio::findById)
                .filter(Usuario::isActivo)
                .orElseThrow(NoAutenticadoException::new);
        return new UsuarioAutenticado(usuario.getId(), usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto(), usuario.getRol(), usuario.permisosEfectivos());
    }

    public boolean verificarPermiso(UsuarioAutenticado usuario, Permiso permiso) {
        return usuario.tienePermiso(permiso);
    }

    /** Exige el permiso al usuario actual: 401 sin sesión, 403 sin permiso. */
    public UsuarioAutenticado exigirPermiso(Permiso permiso) {
        UsuarioAutenticado usuario = verificarIdentidad();
        if (!verificarPermiso(usuario, permiso)) {
            throw new AccesoDenegadoException();
        }
        return usuario;
    }

    private static java.util.Optional<Long> idDe(JwtAuthenticationToken token) {
        try {
            return java.util.Optional.of(Long.valueOf(token.getToken().getSubject()));
        } catch (NumberFormatException e) {
            return java.util.Optional.empty();
        }
    }
}
