package co.edu.humboldt.plataformacasos.sesiones.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dto.HerramientaDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.RegistrarHerramientaRequest;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioHerramientas;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-28
 Para qué sirve: (1) el ESTUDIANTE consulta las herramientas profesionales del caso durante su
 participación; el uso de cada herramienta (calculadora, tablas, etc.) lo ejecuta el frontend con esa
 definición. (2) El DOCENTE creador del caso (permiso GESTIONAR_CASOS) las registra y retira.
 */
@RestController
@RequestMapping("/api")
public class HerramientaControlador {

    private final ServicioHerramientas service;
    private final ServicioAutorizacion autorizacion;

    public HerramientaControlador(ServicioHerramientas service, ServicioAutorizacion autorizacion) {
        this.service = service;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/participaciones/{id}/herramientas")
    public List<HerramientaDto> herramientas(@PathVariable Long id) {
        return service.consultar(autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS).id(), id);
    }

    @GetMapping("/casos/{idCaso}/herramientas")
    public List<HerramientaDto> delCaso(@PathVariable Long idCaso) {
        return service.listarDelCaso(docente(), idCaso);
    }

    @PostMapping("/casos/{idCaso}/herramientas")
    public ResponseEntity<HerramientaDto> registrar(@PathVariable Long idCaso,
                                                    @Valid @RequestBody RegistrarHerramientaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(docente(), idCaso, req));
    }

    @DeleteMapping("/casos/{idCaso}/herramientas/{idHerramienta}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idCaso, @PathVariable Long idHerramienta) {
        service.eliminar(docente(), idCaso, idHerramienta);
        return ResponseEntity.noContent().build();
    }

    private Long docente() {
        return autorizacion.exigirPermiso(Permiso.GESTIONAR_CASOS).id();
    }
}
