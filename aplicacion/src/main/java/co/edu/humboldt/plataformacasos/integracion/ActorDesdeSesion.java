package co.edu.humboldt.plataformacasos.integracion;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.autorizacion.UsuarioAutenticado;
import co.edu.humboldt.plataformacasos.compartido.excepcion.NoAutenticadoException;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActualResolver;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Módulo 1 → módulos 5 y 6: el actor es el usuario del token, leído de la base en cada petición
 * (ServicioAutorizacion.verificarIdentidad), con sus permisos vigentes. En la base única el id del
 * estudiante y del docente es el id de usuario.
 */
@Component
public class ActorDesdeSesion implements ActorActualResolver {

    private final ServicioAutorizacion autorizacion;

    public ActorDesdeSesion(ServicioAutorizacion autorizacion) {
        this.autorizacion = autorizacion;
    }

    @Override
    public Optional<ActorActual> actual() {
        try {
            UsuarioAutenticado usuario = autorizacion.verificarIdentidad();
            return Optional.of(new ActorActual(
                    usuario.id(),
                    usuario.rol() == Rol.ESTUDIANTE ? usuario.id() : null,
                    usuario.rol() == Rol.DOCENTE ? usuario.id() : null,
                    usuario.rol(),
                    usuario.permisos()));
        } catch (NoAutenticadoException e) {
            return Optional.empty();
        }
    }
}
