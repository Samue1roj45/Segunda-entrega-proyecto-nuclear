package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Servicio real del registro de estudiantes (sin controles de acceso: de eso se encarga el proxy).
 * Aplica las mismas validaciones que la creación de docentes y, además, exige el dominio institucional,
 * porque el ingreso del estudiante (módulo 1) rechaza cualquier otro dominio.
 */
@Service
public class ServicioAdministracionEstudiantesImpl implements ServicioAdministracionEstudiantes {

    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LONGITUD_MAXIMA = 150;

    private final UsuarioRepositorio usuarioRepositorio;
    private final java.util.List<String> dominios;
    private final String dominio;

    public ServicioAdministracionEstudiantesImpl(UsuarioRepositorio usuarioRepositorio,
            @Value("${app.autenticacion.dominio-institucional}") String dominioInstitucional) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.dominios = java.util.Arrays.stream(dominioInstitucional.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(d -> !d.isEmpty()).toList();
        this.dominio = String.join(" o @", dominios);
    }

    // Opcionales: las pruebas unitarias crean el servicio sin ellos.
    @Autowired(required = false)
    private ServicioAvisos avisos;
    @Autowired(required = false)
    private ServicioAutorizacion autorizacion;

    @Override
    @Transactional
    public EstudianteDTO crearEstudiante(CrearEstudianteRequest datos) {
        String nombre = datos == null ? null : datos.nombreCompleto();
        String correo = datos == null ? null : datos.correoInstitucional();
        if (nombre == null || nombre.isBlank()) {
            throw new SolicitudInvalidaException(MensajesAdministracion.NOMBRE_OBLIGATORIO);
        }
        if (correo == null || correo.isBlank()) {
            throw new SolicitudInvalidaException(MensajesAdministracion.CORREO_OBLIGATORIO);
        }
        String correoNormalizado = Usuario.normalizarCorreo(correo);
        if (!FORMATO_CORREO.matcher(correoNormalizado).matches()
                || correoNormalizado.length() > LONGITUD_MAXIMA || nombre.trim().length() > LONGITUD_MAXIMA) {
            throw new SolicitudInvalidaException(MensajesAdministracion.CORREO_INVALIDO);
        }
        if (dominios.stream().noneMatch(d -> correoNormalizado.endsWith("@" + d))) {
            throw new SolicitudInvalidaException("El correo del estudiante debe ser del dominio institucional @"
                    + dominio + "; con otro dominio no podría ingresar.");
        }
        if (usuarioRepositorio.existsByCorreoInstitucional(correoNormalizado)) {
            throw new ConflictoException(MensajesAdministracion.CORREO_DUPLICADO);
        }
        Usuario estudiante = usuarioRepositorio.save(Usuario.nuevoEstudiante(correoNormalizado, nombre.trim()));
        dejarConstancia("Se registró el estudiante " + estudiante.getNombreCompleto() + " (" + correoNormalizado + ")");
        return EstudianteDTO.de(estudiante);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstudianteDTO> listarEstudiantes() {
        return usuarioRepositorio.findByRolOrderByNombreCompletoAsc(Rol.ESTUDIANTE).stream()
                .map(EstudianteDTO::de)
                .toList();
    }

    /** Auditoría y aviso a los administradores, igual que al crear un docente. */
    private void dejarConstancia(String detalle) {
        if (avisos == null) {
            return;
        }
        Long actorId = null;
        String actorCorreo = null;
        if (autorizacion != null) {
            try {
                var yo = autorizacion.verificarIdentidad();
                actorId = yo.id();
                actorCorreo = yo.correoInstitucional();
            } catch (RuntimeException e) {
                // sin sesión: se registra sin autor
            }
        }
        avisos.registrar("ESTUDIANTE_CREADO", actorId, actorCorreo, detalle);
        avisos.avisarAdministradores("ADMINISTRACION", "Cambio en estudiantes", detalle + ".");
    }
}
