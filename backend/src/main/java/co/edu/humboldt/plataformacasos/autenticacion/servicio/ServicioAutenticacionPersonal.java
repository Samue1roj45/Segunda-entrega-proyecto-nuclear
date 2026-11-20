package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión del personal (docentes y administrador) con correo y contraseña.
 * Cualquier fallo de credenciales responde el mismo mensaje genérico.
 */
@Service
public class ServicioAutenticacionPersonal {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;
    private final EmisorToken emisorToken;
    private final ControlAcceso control;
    private final ServicioAvisos avisos;

    public ServicioAutenticacionPersonal(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificador,
                                         EmisorToken emisorToken, ControlAcceso control, ServicioAvisos avisos) {
        this.control = control;
        this.avisos = avisos;
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.emisorToken = emisorToken;
    }

    @Transactional(readOnly = true)
    public SesionUsuario autenticar(String correoInstitucional, String clave) {
        if (correoInstitucional == null || correoInstitucional.isBlank() || clave == null || clave.isBlank()) {
            throw new SolicitudInvalidaException(MensajesAutenticacion.CORREO_Y_CLAVE_OBLIGATORIOS);
        }
        control.exigirLibre(correoInstitucional);
        Usuario usuario = usuarioRepositorio.findByCorreoInstitucional(Usuario.normalizarCorreo(correoInstitucional))
                .filter(u -> u.esPersonal() && u.isActivo() && u.getClaveHash() != null)
                .filter(u -> codificador.matches(clave, u.getClaveHash()))
                .orElse(null);
        if (usuario == null) {
            control.registrarFallo(correoInstitucional);
            throw new CredencialesInvalidasException(MensajesAutenticacion.CREDENCIALES_PERSONAL_INVALIDAS);
        }
        control.registrarExito(correoInstitucional);
        avisos.registrar("INGRESO", usuario.getId(), usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto() + " inició sesión (" + usuario.getRol() + ").");
        return emisorToken.emitir(usuario);
    }
}
