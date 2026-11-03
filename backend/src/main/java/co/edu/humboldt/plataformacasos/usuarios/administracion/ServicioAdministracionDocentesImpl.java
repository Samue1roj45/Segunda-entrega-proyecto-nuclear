package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Servicio real de RF-03 (sin controles de acceso: de eso se encarga el proxy).
 * Las reglas de permisos asignables viven en la entidad {@link Usuario}.
 */
@Service
public class ServicioAdministracionDocentesImpl implements ServicioAdministracionDocentes {

    private static final Pattern FORMATO_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int LONGITUD_MAXIMA = 150;

    private final UsuarioRepositorio usuarioRepositorio;

    public ServicioAdministracionDocentesImpl(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    @Transactional
    public DocenteDTO crearDocente(CrearDocenteRequest datos) {
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
        if (usuarioRepositorio.existsByCorreoInstitucional(correoNormalizado)) {
            throw new ConflictoException(MensajesAdministracion.CORREO_DUPLICADO);
        }
        Usuario docente = usuarioRepositorio.save(Usuario.nuevoDocente(correoNormalizado, nombre));
        return DocenteDTO.de(docente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocenteDTO> listarDocentes() {
        return usuarioRepositorio.findByRolOrderByNombreCompletoAsc(Rol.DOCENTE).stream()
                .map(DocenteDTO::de)
                .toList();
    }

    @Override
    @Transactional
    public DocenteDTO asignarPermisos(Long docenteId, Set<Permiso> permisos) {
        if (permisos == null || permisos.stream().anyMatch(Objects::isNull)) {
            throw new SolicitudInvalidaException(MensajesAdministracion.PERMISOS_OBLIGATORIOS);
        }
        Usuario docente = usuarioRepositorio.findById(docenteId)
                .filter(u -> u.getRol() == Rol.DOCENTE)
                .orElseThrow(() -> new RecursoNoEncontradoException(MensajesAdministracion.DOCENTE_NO_ENCONTRADO));
        try {
            docente.asignarPermisos(permisos);
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException(e.getMessage());
        }
        return DocenteDTO.de(docente);
    }
}
