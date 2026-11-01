package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.Validador;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * RF-01: autentica estudiantes con correo institucional y código de autorización.
 * No contiene reglas de validación: las delega a las cadenas configuradas.
 */
@Service
public class ServicioAutenticacion {

    private final Validador<SolicitudAutenticacion> validacionFormato;
    private final Validador<IntentoAutenticacion> validacionCredenciales;
    private final UsuarioRepositorio usuarioRepositorio;
    private final EmisorToken emisorToken;

    public ServicioAutenticacion(Validador<SolicitudAutenticacion> validacionFormatoAutenticacion,
                                 Validador<IntentoAutenticacion> validacionCredencialesEstudiante,
                                 UsuarioRepositorio usuarioRepositorio,
                                 EmisorToken emisorToken) {
        this.validacionFormato = validacionFormatoAutenticacion;
        this.validacionCredenciales = validacionCredencialesEstudiante;
        this.usuarioRepositorio = usuarioRepositorio;
        this.emisorToken = emisorToken;
    }

    @Transactional(readOnly = true)
    public SesionUsuario autenticarEstudiante(SolicitudAutenticacion solicitud) {
        ResultadoValidacion formato = validacionFormato.validar(solicitud);
        if (!formato.aprobado()) {
            throw new SolicitudInvalidaException(formato.mensaje());
        }

        Optional<Usuario> usuario = usuarioRepositorio.findByCorreoInstitucional(
                Usuario.normalizarCorreo(solicitud.correoInstitucional()));
        IntentoAutenticacion intento = new IntentoAutenticacion(usuario, solicitud.codigoAutorizacion());
        if (!validacionCredenciales.validar(intento).aprobado()) {
            throw new CredencialesInvalidasException();
        }
        return emisorToken.emitir(usuario.orElseThrow());
    }
}
