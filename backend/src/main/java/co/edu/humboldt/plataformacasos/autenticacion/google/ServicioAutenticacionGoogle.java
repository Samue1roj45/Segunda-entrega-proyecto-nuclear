package co.edu.humboldt.plataformacasos.autenticacion.google;

import co.edu.humboldt.plataformacasos.autenticacion.servicio.CredencialesInvalidasException;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Ingreso con Google. Solo entra quien YA tiene cuenta en SOMA (docentes y administrador creados por el
 * administrador; estudiantes que aceptaron una invitación) y cuyo correo es de un dominio permitido.
 */
@Service
public class ServicioAutenticacionGoogle {

    static final String NO_CONFIGURADO = "El ingreso con Google no está configurado en este servidor.";
    static final String INVALIDO = "No se pudo validar tu cuenta de Google. Inténtalo de nuevo.";
    static final String DOMINIO = "Solo se permiten correos de la Universidad Humboldt (@unihumboldt.edu.co o @cue.edu.co).";
    static final String SIN_CUENTA = "Ese correo todavía no tiene cuenta en SOMA. Pídele una invitación a tu docente.";

    private final PropiedadesGoogle propiedades;
    private final VerificadorGoogle verificador;
    private final UsuarioRepositorio usuarios;
    private final EmisorToken emisor;
    private final ServicioAvisos avisos;

    public ServicioAutenticacionGoogle(PropiedadesGoogle propiedades, VerificadorGoogle verificador,
                                       UsuarioRepositorio usuarios, EmisorToken emisor, ServicioAvisos avisos) {
        this.propiedades = propiedades;
        this.verificador = verificador;
        this.usuarios = usuarios;
        this.emisor = emisor;
        this.avisos = avisos;
    }

    @Transactional(readOnly = true)
    public SesionUsuario autenticar(String credencial) {
        if (!propiedades.configurado()) { throw new SolicitudInvalidaException(NO_CONFIGURADO); }
        if (credencial == null || credencial.isBlank()) { throw new SolicitudInvalidaException(INVALIDO); }
        var identidad = verificador.verificar(credencial.trim())
                .filter(VerificadorGoogle.IdentidadGoogle::correoVerificado)
                .orElseThrow(() -> new CredencialesInvalidasException(INVALIDO));
        String correo = Usuario.normalizarCorreo(identidad.correo());
        String dominio = correo.substring(correo.indexOf('@') + 1).toLowerCase(Locale.ROOT);
        if (!propiedades.dominios().contains(dominio)) { throw new CredencialesInvalidasException(DOMINIO); }
        Usuario usuario = usuarios.findByCorreoInstitucional(correo).filter(Usuario::isActivo)
                .orElseThrow(() -> new CredencialesInvalidasException(SIN_CUENTA));
        avisos.registrar("INGRESO", usuario.getId(), usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto() + " ingresó con Google (" + usuario.getRol() + ").");
        if (!usuario.esPersonal()) {
            avisos.avisar(avisos.docentesDelEstudiante(usuario.getId()), "INGRESO_ESTUDIANTE", "Un estudiante ingresó",
                    usuario.getNombreCompleto() + " ingresó a la plataforma.");
        }
        return emisor.emitir(usuario);
    }
}
