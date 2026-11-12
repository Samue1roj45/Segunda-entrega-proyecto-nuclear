package co.edu.humboldt.plataformacasos.sesiones.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dto.AgendarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.ConvocadoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ModificarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionCreadaResponse;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import co.edu.humboldt.plataformacasos.sesiones.fachada.FachadaSesiones;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Para qué sirve: API REST del DOCENTE para agendar sesiones, consultarlas, cambiar sus condiciones,
 administrar los estudiantes autorizados e iniciar/finalizar la sesión. Solo habla con la Fachada.
 El docente es el usuario del token del módulo 1 y debe tener el permiso PROGRAMAR_SESIONES (RF-02).
 */
@RestController
@RequestMapping("/api/sesiones")
public class SesionControlador {

    private final FachadaSesiones facade;
    private final ServicioAutorizacion autorizacion;

    public SesionControlador(FachadaSesiones facade, ServicioAutorizacion autorizacion) {
        this.facade = facade;
        this.autorizacion = autorizacion;
    }

    @PostMapping                                                    // RF-04 + RF-18
    public ResponseEntity<SesionCreadaResponse> agendar(@Valid @RequestBody AgendarSesionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.agendar(docente(), req));
    }

    @GetMapping                                                     // RF-18: mis sesiones
    public List<SesionDto> listar(@RequestParam(required = false) String estado) {
        return facade.listarSesiones(docente(), estado);
    }

    @GetMapping("/{id}")                                            // RF-18: detalle
    public SesionDto obtener(@PathVariable Long id) {
        return facade.obtenerSesion(id, docente());
    }

    @PutMapping("/{id}")                                            // RF-18: cambiar condiciones
    public SesionDto modificar(@PathVariable Long id, @Valid @RequestBody ModificarSesionRequest req) {
        return facade.modificarSesion(id, docente(), req);
    }

    @PostMapping("/{id}/iniciar")                                   // RF-19: SIN_INICIAR -> EN_CURSO
    public SesionDto iniciar(@PathVariable Long id) {
        return facade.iniciarSesion(id, docente());
    }

    @PostMapping("/{id}/finalizar")                                 // RF-19: EN_CURSO -> FINALIZADO
    public SesionDto finalizar(@PathVariable Long id) {
        return facade.finalizarSesion(id, docente());
    }

    @GetMapping("/{id}/estudiantes")                                // RF-04
    public List<ConvocadoDto> listarEstudiantes(@PathVariable Long id) {
        return facade.listarConvocados(id, docente());
    }

    @PostMapping("/{id}/estudiantes/{idEstudiante}")                // RF-04
    public ResponseEntity<Map<String, Long>> agregar(@PathVariable Long id, @PathVariable Long idEstudiante) {
        Long aut = facade.agregarEstudiante(id, docente(), idEstudiante);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("idAutorizacion", aut));
    }

    @DeleteMapping("/{id}/estudiantes/{idEstudiante}")              // RF-04
    public ResponseEntity<Void> retirar(@PathVariable Long id, @PathVariable Long idEstudiante) {
        facade.retirarEstudiante(id, docente(), idEstudiante);
        return ResponseEntity.noContent().build();
    }

    private Long docente() {
        return autorizacion.exigirPermiso(Permiso.PROGRAMAR_SESIONES).id();
    }
}
