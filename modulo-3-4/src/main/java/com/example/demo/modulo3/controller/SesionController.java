package com.example.demo.modulo3.controller;

import com.example.demo.dto.AgendarSesionRequest;
import com.example.demo.dto.ConvocadoDto;
import com.example.demo.dto.SesionCreadaResponse;
import com.example.demo.dto.SesionDto;
import com.example.demo.facade.Facade;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Para qué sirve: API REST del DOCENTE para agendar sesiones, consultarlas, administrar los
 estudiantes autorizados e iniciar/finalizar la sesión. Solo habla con la Fachada.
 cuando exista autenticación (RF-01/02) se toma del
 usuario autenticado y esta cabecera desaparece.
 */
@RestController
@RequestMapping("/api/sesiones")
public class SesionController {

    private static final String DOCENTE = "X-Docente-Id";
    private final Facade facade;

    public SesionController(Facade facade) {
        this.facade = facade;
    }

    @PostMapping                                                    // RF-04 + RF-18
    public ResponseEntity<SesionCreadaResponse> agendar(@RequestHeader(DOCENTE) Long idDocente,
                                                        @Valid @RequestBody AgendarSesionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.agendar(idDocente, req));
    }

    @GetMapping                                                     // RF-18: mis sesiones
    public List<SesionDto> listar(@RequestHeader(DOCENTE) Long idDocente,
                                  @RequestParam(required = false) String estado) {
        return facade.listarSesiones(idDocente, estado);
    }

    @GetMapping("/{id}")                                            // RF-18: detalle
    public SesionDto obtener(@PathVariable Long id, @RequestHeader(DOCENTE) Long idDocente) {
        return facade.obtenerSesion(id, idDocente);
    }

    @PostMapping("/{id}/iniciar")                                   // RF-19: SIN_INICIAR -> EN_CURSO
    public SesionDto iniciar(@PathVariable Long id, @RequestHeader(DOCENTE) Long idDocente) {
        return facade.iniciarSesion(id, idDocente);
    }

    @PostMapping("/{id}/finalizar")                                 // RF-19: EN_CURSO -> FINALIZADO
    public SesionDto finalizar(@PathVariable Long id, @RequestHeader(DOCENTE) Long idDocente) {
        return facade.finalizarSesion(id, idDocente);
    }

    @GetMapping("/{id}/estudiantes")                                // RF-04
    public List<ConvocadoDto> listarEstudiantes(@PathVariable Long id,
                                                @RequestHeader(DOCENTE) Long idDocente) {
        return facade.listarConvocados(id, idDocente);
    }

    @PostMapping("/{id}/estudiantes/{idEstudiante}")                // RF-04
    public ResponseEntity<Map<String, Long>> agregar(@PathVariable Long id,
                                                     @PathVariable Long idEstudiante,
                                                     @RequestHeader(DOCENTE) Long idDocente) {
        Long aut = facade.agregarEstudiante(id, idDocente, idEstudiante);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("idAutorizacion", aut));
    }

    @DeleteMapping("/{id}/estudiantes/{idEstudiante}")              // RF-04
    public ResponseEntity<Void> retirar(@PathVariable Long id,
                                        @PathVariable Long idEstudiante,
                                        @RequestHeader(DOCENTE) Long idDocente) {
        facade.retirarEstudiante(id, idDocente, idEstudiante);
        return ResponseEntity.noContent().build();
    }
}
