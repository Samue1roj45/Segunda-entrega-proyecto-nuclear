package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.autorizacion.UsuarioAutenticado;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio real de la administración de cuentas (sin controles de acceso: de eso se encarga el proxy).
 * Las reglas de seguridad están en las cadenas de {@link CadenasAdministracionCuentas}; el historial que
 * impide borrar lo informa el puerto {@link RegistrosHistoricosUsuario}; el cambio de estado, la entidad.
 * La auditoría de las operaciones exitosas la hace {@link ObservadorCuentasAdministradas} tras el commit;
 * los intentos bloqueados se registran aquí mismo, porque esa transacción se deshace.
 */
@Service
public class ServicioAdministracionUsuariosImpl implements ServicioAdministracionUsuarios {

    private final UsuarioRepositorio usuarios;
    private final CadenasAdministracionCuentas cadenas;
    private final RegistrosHistoricosUsuario registros;
    private final ServicioAutorizacion autorizacion;
    private final ServicioAvisos avisos;
    private final ApplicationEventPublisher eventos;

    public ServicioAdministracionUsuariosImpl(UsuarioRepositorio usuarios, CadenasAdministracionCuentas cadenas,
                                              RegistrosHistoricosUsuario registros, ServicioAutorizacion autorizacion,
                                              ServicioAvisos avisos, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.cadenas = cadenas;
        this.registros = registros;
        this.autorizacion = autorizacion;
        this.avisos = avisos;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioAdminDTO> listar(boolean activos, Rol rol) {
        return usuarios.listarPorEstado(activos, rol).stream().map(UsuarioAdminDTO::de).toList();
    }

    @Override
    @Transactional
    public UsuarioAdminDTO archivar(Long idUsuario) {
        UsuarioAutenticado actor = autorizacion.verificarIdentidad();
        Usuario cuenta = cuentaParaModificar(idUsuario);
        if (!cuenta.isActivo()) {
            throw new ConflictoException(MensajesAdministracion.YA_ARCHIVADA);
        }
        exigir(cadenas.archivo().validar(new SolicitudCambioCuenta(cuenta, actor.id())));
        cuenta.archivar();
        eventos.publishEvent(CuentaAdministradaEvento.sinAvisoPropio("USUARIO_ARCHIVADO", actor.id(),
                actor.correoInstitucional(), actor.nombreCompleto() + " archivó la cuenta de " + describir(cuenta)
                        + ". Ya no puede iniciar sesión; su historial se conserva"));
        return UsuarioAdminDTO.de(cuenta);
    }

    @Override
    @Transactional
    public UsuarioAdminDTO reactivar(Long idUsuario) {
        UsuarioAutenticado actor = autorizacion.verificarIdentidad();
        Usuario cuenta = cuentaParaModificar(idUsuario);
        if (cuenta.isActivo()) {
            throw new ConflictoException(MensajesAdministracion.YA_ACTIVA);
        }
        cuenta.reactivar();
        eventos.publishEvent(new CuentaAdministradaEvento("USUARIO_REACTIVADO", actor.id(), actor.correoInstitucional(),
                actor.nombreCompleto() + " reactivó la cuenta de " + describir(cuenta), cuenta.getId(),
                "Tu cuenta está activa", "El administrador reactivó tu cuenta de SOMA. Ya puedes volver a iniciar sesión."));
        return UsuarioAdminDTO.de(cuenta);
    }

    /** Ejecuta la misma cadena que la eliminación, dentro de una transacción corta, sin cambiar nada. */
    @Override
    @Transactional
    public EvaluacionEliminacionDTO evaluarEliminacion(Long idUsuario) {
        UsuarioAutenticado actor = autorizacion.verificarIdentidad();
        Usuario cuenta = usuarios.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(MensajesAdministracion.USUARIO_NO_ENCONTRADO));
        ResultadoValidacion resultado = cadenas.eliminacion().validar(new SolicitudCambioCuenta(cuenta, actor.id()));
        return new EvaluacionEliminacionDTO(resultado.aprobado(), resultado.mensaje(), registros.de(cuenta.getId()));
    }

    @Override
    @Transactional
    public void eliminar(Long idUsuario) {
        UsuarioAutenticado actor = autorizacion.verificarIdentidad();
        Usuario cuenta = cuentaParaModificar(idUsuario);
        String descripcion = describir(cuenta);
        ResultadoValidacion resultado = cadenas.eliminacion().validar(new SolicitudCambioCuenta(cuenta, actor.id()));
        if (!resultado.aprobado()) {
            avisos.registrar("USUARIO_ELIMINACION_BLOQUEADA", actor.id(), actor.correoInstitucional(),
                    actor.nombreCompleto() + " intentó eliminar la cuenta de " + descripcion + ". Bloqueado: " + resultado.mensaje());
            throw new ConflictoException(resultado.mensaje());
        }
        try {
            usuarios.delete(cuenta);
            usuarios.flush();
        } catch (DataIntegrityViolationException e) {
            // Otro proceso creó un registro con llave hacia la cuenta entre la validación y el borrado.
            avisos.registrar("USUARIO_ELIMINACION_BLOQUEADA", actor.id(), actor.correoInstitucional(),
                    actor.nombreCompleto() + " intentó eliminar la cuenta de " + descripcion
                            + ". Bloqueado por registros asociados en la base de datos");
            throw new ConflictoException(MensajesAdministracion.REGISTROS_CONCURRENTES);
        }
        eventos.publishEvent(CuentaAdministradaEvento.sinAvisoPropio("USUARIO_ELIMINADO", actor.id(),
                actor.correoInstitucional(), actor.nombreCompleto() + " eliminó definitivamente la cuenta de " + descripcion
                        + ". No tenía registros académicos ni comunicaciones"));
    }

    /**
     * Bloquea siempre en el mismo orden (primero los administradores activos, luego la cuenta): dos
     * administradores que se archivan entre sí quedan en fila en lugar de bloquearse mutuamente.
     */
    private Usuario cuentaParaModificar(Long idUsuario) {
        usuarios.bloquearAdministradoresActivos();
        return usuarios.buscarParaModificar(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(MensajesAdministracion.USUARIO_NO_ENCONTRADO));
    }

    private static void exigir(ResultadoValidacion resultado) {
        if (!resultado.aprobado()) {
            throw new ConflictoException(resultado.mensaje());
        }
    }

    private static String describir(Usuario cuenta) {
        return cuenta.getNombreCompleto() + " (" + cuenta.getCorreoInstitucional() + ", "
                + cuenta.getRol().name().toLowerCase(java.util.Locale.ROOT) + ", id " + cuenta.getId() + ")";
    }
}
