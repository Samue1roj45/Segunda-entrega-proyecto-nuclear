package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.usuarios.administracion.AsignarPermisosRequest;
import co.edu.humboldt.plataformacasos.usuarios.administracion.CrearDocenteRequest;
import co.edu.humboldt.plataformacasos.usuarios.administracion.DocenteDTO;
import co.edu.humboldt.plataformacasos.usuarios.administracion.ServicioAdministracionDocentes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** RF-03: administración de docentes y permisos. El acceso lo controla el proxy del servicio. */
@RestController
@RequestMapping("/api/admin/docentes")
public class AdministracionDocentesControlador {

    private final ServicioAdministracionDocentes servicio;

    public AdministracionDocentesControlador(ServicioAdministracionDocentes servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<DocenteDTO> listar() {
        return servicio.listarDocentes();
    }

    @PostMapping
    public ResponseEntity<DocenteDTO> crear(@RequestBody CrearDocenteRequest datos) {
        DocenteDTO creado = servicio.crearDocente(datos);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}/permisos")
    public DocenteDTO asignarPermisos(@PathVariable Long id, @RequestBody AsignarPermisosRequest solicitud) {
        return servicio.asignarPermisos(id, solicitud.permisos());
    }
}
