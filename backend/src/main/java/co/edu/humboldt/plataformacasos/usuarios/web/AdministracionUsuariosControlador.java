package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.usuarios.administracion.EvaluacionEliminacionDTO;
import co.edu.humboldt.plataformacasos.usuarios.administracion.ServicioAdministracionUsuarios;
import co.edu.humboldt.plataformacasos.usuarios.administracion.UsuarioAdminDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administración de cuentas de docentes, estudiantes y administradores. El acceso lo controla el proxy
 * del servicio con el permiso administrativo existente; aquí solo se traduce HTTP.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdministracionUsuariosControlador {

    private final ServicioAdministracionUsuarios servicio;

    public AdministracionUsuariosControlador(ServicioAdministracionUsuarios servicio) {
        this.servicio = servicio;
    }

    /** {@code activos=false} lista las cuentas archivadas. {@code rol} es opcional. */
    @GetMapping
    public List<UsuarioAdminDTO> listar(@RequestParam(defaultValue = "true") boolean activos,
                                        @RequestParam(required = false) Rol rol) {
        return servicio.listar(activos, rol);
    }

    @PostMapping("/{id}/archivo")
    public UsuarioAdminDTO archivar(@PathVariable Long id) {
        return servicio.archivar(id);
    }

    @PostMapping("/{id}/reactivacion")
    public UsuarioAdminDTO reactivar(@PathVariable Long id) {
        return servicio.reactivar(id);
    }

    @GetMapping("/{id}/eliminacion")
    public EvaluacionEliminacionDTO evaluarEliminacion(@PathVariable Long id) {
        return servicio.evaluarEliminacion(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
