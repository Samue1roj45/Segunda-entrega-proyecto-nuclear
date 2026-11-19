package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Listado de estudiantes activos para que el docente los elija al convocar una sesión (RF-04).
 * Solo lo puede consultar quien tiene PROGRAMAR_SESIONES; no expone códigos ni datos sensibles.
 */
@RestController
@RequestMapping("/api/estudiantes")
public class EstudiantesControlador {

    private final UsuarioRepositorio usuarios;
    private final ServicioAutorizacion autorizacion;

    public EstudiantesControlador(UsuarioRepositorio usuarios, ServicioAutorizacion autorizacion) {
        this.usuarios = usuarios;
        this.autorizacion = autorizacion;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EstudianteResumenDto> listar() {
        autorizacion.exigirPermiso(Permiso.PROGRAMAR_SESIONES);
        return usuarios.findByRolOrderByNombreCompletoAsc(Rol.ESTUDIANTE).stream()
                .filter(u -> u.isActivo())
                .map(u -> new EstudianteResumenDto(u.getId(), u.getNombreCompleto(), u.getCorreoInstitucional()))
                .toList();
    }
}
