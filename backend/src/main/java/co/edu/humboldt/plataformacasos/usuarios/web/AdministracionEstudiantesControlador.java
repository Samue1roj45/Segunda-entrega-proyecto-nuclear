package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.usuarios.administracion.CrearEstudianteRequest;
import co.edu.humboldt.plataformacasos.usuarios.administracion.EstudianteDTO;
import co.edu.humboldt.plataformacasos.usuarios.administracion.ServicioAdministracionEstudiantes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Registro de estudiantes por el administrador, para que los docentes puedan convocarlos (RF-04).
 * El acceso lo controla el proxy del servicio (permiso ADMINISTRAR_DOCENTES).
 */
@RestController
@RequestMapping("/api/admin/estudiantes")
public class AdministracionEstudiantesControlador {

    private final ServicioAdministracionEstudiantes servicio;

    public AdministracionEstudiantesControlador(ServicioAdministracionEstudiantes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<EstudianteDTO> listar() {
        return servicio.listarEstudiantes();
    }

    @PostMapping
    public ResponseEntity<EstudianteDTO> crear(@RequestBody CrearEstudianteRequest datos) {
        EstudianteDTO creado = servicio.crearEstudiante(datos);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }
}
