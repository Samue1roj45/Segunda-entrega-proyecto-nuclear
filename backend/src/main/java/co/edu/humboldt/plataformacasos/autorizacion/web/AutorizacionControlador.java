package co.edu.humboldt.plataformacasos.autorizacion.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.autorizacion.UsuarioAutenticado;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** RF-02: consulta de la sesión y verificación de permisos en el servidor. */
@RestController
public class AutorizacionControlador {

    private final ServicioAutorizacion servicioAutorizacion;

    public AutorizacionControlador(ServicioAutorizacion servicioAutorizacion) {
        this.servicioAutorizacion = servicioAutorizacion;
    }

    /** Usuario actual con su rol y permisos vigentes: base para mostrar las opciones de cada rol. */
    @GetMapping("/api/sesion")
    public UsuarioAutenticado sesionActual() {
        return servicioAutorizacion.verificarIdentidad();
    }

    /** 204 si el usuario actual tiene el permiso; 403 si no; 401 sin sesión. */
    @GetMapping("/api/autorizacion/permisos/{permiso}")
    public ResponseEntity<Void> verificarPermiso(@PathVariable Permiso permiso) {
        servicioAutorizacion.exigirPermiso(permiso);
        return ResponseEntity.noContent().build();
    }
}
