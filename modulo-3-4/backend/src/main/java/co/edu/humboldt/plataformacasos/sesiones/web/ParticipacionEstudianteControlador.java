package com.example.demo.modulo3.controller;

import com.example.demo.dto.ContextoDto;
import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.dto.FinalizacionDto;
import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.ResponderRequest;
import com.example.demo.dto.TiempoRestanteDto;
import com.example.demo.service.ParticipacionService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Para qué sirve: API REST del ESTUDIANTE durante su participación: ver si puede ingresar,
 iniciar, ver contextos y preguntas, responder/cambiar respuestas, ver progreso y tiempo
 restante, y finalizar su intento. Recibe la interfaz ParticipacionService; Spring inyecta
 el Proxy (@Primary).
 */
@RestController
@RequestMapping("/api")
public class ParticipacionController {

    private static final String ESTUDIANTE = "X-Estudiante-Id";
    private final ParticipacionService service;

    public ParticipacionController(ParticipacionService service) {
        this.service = service;
    }

    @GetMapping("/autorizaciones/{idAutorizacion}/ingreso")         // RF-22
    public EstadoIngresoDto ingreso(@PathVariable Long idAutorizacion,
                                    @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        return service.consultarIngreso(idEstudiante, idAutorizacion);
    }

    @PostMapping("/autorizaciones/{idAutorizacion}/participaciones")   // RF-10 / RF-22 / RF-25
    public ResponseEntity<Map<String, Long>> iniciar(@PathVariable Long idAutorizacion,
                                                     @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        Long id = service.iniciar(idEstudiante, idAutorizacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("idParticipacion", id));
    }

    @GetMapping("/participaciones/{id}/contexto")                   // RF-10
    public ContextoDto contexto(@PathVariable Long id, @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        return service.consultarContexto(idEstudiante, id);
    }

    @PutMapping("/participaciones/{id}/preguntas/{idPregunta}/respuesta")   // RF-10 + RF-11
    public ProgresoDto responder(@PathVariable Long id, @PathVariable Long idPregunta,
                                 @RequestHeader(ESTUDIANTE) Long idEstudiante,
                                 @Valid @RequestBody ResponderRequest req) {
        return service.responder(idEstudiante, id, idPregunta, req.idRespuesta());
    }

    @GetMapping("/participaciones/{id}/progreso")                   // RF-11
    public ProgresoDto progreso(@PathVariable Long id, @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        return service.consultarProgreso(idEstudiante, id);
    }

    @GetMapping("/participaciones/{id}/tiempo")                     // RF-23
    public TiempoRestanteDto tiempo(@PathVariable Long id, @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        return service.consultarTiempo(idEstudiante, id);
    }

    @PostMapping("/participaciones/{id}/finalizar")                 // RF-23 / RF-25
    public FinalizacionDto finalizar(@PathVariable Long id, @RequestHeader(ESTUDIANTE) Long idEstudiante) {
        return service.finalizar(idEstudiante, id);
    }
}
