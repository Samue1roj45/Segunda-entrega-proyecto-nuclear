package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
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
    private final ControlAcceso control;
    private final ServicioAvisos avisos;

    public ServicioAutenticacion(Validador<SolicitudAutenticacion> validacionFormatoAutenticacion,
                                 Validador<IntentoAutenticacion> validacionCredencialesEstudiante,
                                 UsuarioRepositorio usuarioRepositorio,
                                 EmisorToken emisorToken, ControlAcceso control, ServicioAvisos avisos) {
        this.control = control;
        this.avisos = avisos;
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
        control.exigirLibre(solicitud.correoInstitucional());
        if (!validacionCredenciales.validar(intento).aprobado()) {
            control.registrarFallo(solicitud.correoInstitucional());
            throw new CredencialesInvalidasException();
        }
        Usuario estudiante = usuario.orElseThrow();
        control.registrarExito(solicitud.correoInstitucional());
        avisos.registrar("INGRESO", estudiante.getId(), estudiante.getCorreoInstitucional(),
                estudiante.getNombreCompleto() + " ingresó a la plataforma (ESTUDIANTE).");
        avisos.avisar(avisos.docentesDelEstudiante(estudiante.getId()), "INGRESO_ESTUDIANTE", "Un estudiante ingresó",
                estudiante.getNombreCompleto() + " ingresó a la plataforma.");
        return emisorToken.emitir(estudiante);
    }
}
