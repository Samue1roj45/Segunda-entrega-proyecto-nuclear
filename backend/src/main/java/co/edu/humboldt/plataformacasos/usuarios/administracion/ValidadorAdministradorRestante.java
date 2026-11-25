package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;

/**
 * Eslabón: el sistema nunca queda sin un administrador activo. Bloquea las filas de los administradores
 * activos, así que debe ejecutarse dentro de la transacción que hace el cambio.
 */
public class ValidadorAdministradorRestante extends ValidadorBase<SolicitudCambioCuenta> {

    private final UsuarioRepositorio usuarios;

    public ValidadorAdministradorRestante(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudCambioCuenta solicitud) {
        var cuenta = solicitud.cuenta();
        if (cuenta.getRol() != Rol.ADMINISTRADOR || !cuenta.isActivo()) {
            return ResultadoValidacion.exito();
        }
        long otrosActivos = usuarios.bloquearAdministradoresActivos().stream()
                .filter(a -> !a.getId().equals(cuenta.getId()))
                .count();
        return otrosActivos == 0
                ? ResultadoValidacion.rechazo(MensajesAdministracion.ULTIMO_ADMINISTRADOR)
                : ResultadoValidacion.exito();
    }
}
