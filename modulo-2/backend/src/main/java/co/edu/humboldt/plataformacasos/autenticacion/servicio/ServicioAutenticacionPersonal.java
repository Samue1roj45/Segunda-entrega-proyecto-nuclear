package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
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

    public ServicioAutenticacionPersonal(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificador,
                                         EmisorToken emisorToken) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
        this.emisorToken = emisorToken;
    }

    @Transactional(readOnly = true)
    public SesionUsuario autenticar(String correoInstitucional, String clave) {
        if (correoInstitucional == null || correoInstitucional.isBlank() || clave == null || clave.isBlank()) {
            throw new SolicitudInvalidaException(MensajesAutenticacion.CORREO_Y_CLAVE_OBLIGATORIOS);
        }
        Usuario usuario = usuarioRepositorio.findByCorreoInstitucional(Usuario.normalizarCorreo(correoInstitucional))
                .filter(u -> u.esPersonal() && u.isActivo() && u.getClaveHash() != null)
                .filter(u -> codificador.matches(clave, u.getClaveHash()))
                .orElseThrow(() -> new CredencialesInvalidasException(
                        MensajesAutenticacion.CREDENCIALES_PERSONAL_INVALIDAS));
        return emisorToken.emitir(usuario);
    }
}
