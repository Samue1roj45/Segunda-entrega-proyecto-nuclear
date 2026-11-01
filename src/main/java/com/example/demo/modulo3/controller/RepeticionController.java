package com.example.demo.modulo3.controller;

import com.example.demo.dto.AutorizarRepeticionRequest;
import com.example.demo.dto.IntentosDto;
import com.example.demo.service.RepeticionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-25
 Para qué sirve: API REST del DOCENTE para conceder un intento adicional a un estudiante
 (repetición excepcional) y ver cuántos intentos lleva. Solo el docente creador del caso
 puede autorizar.
 */
@RestController
@RequestMapping("/api/autorizaciones")
public class RepeticionController {

    private static final String DOCENTE = "X-Docente-Id";
    private final RepeticionService service;

    public RepeticionController(RepeticionService service) {
        this.service = service;
    }

    @PostMapping("/{idAutorizacion}/repeticiones")
    public ResponseEntity<IntentosDto> autorizar(@PathVariable Long idAutorizacion,
                                                 @RequestHeader(DOCENTE) Long idDocente,
                                                 @Valid @RequestBody(required = false) AutorizarRepeticionRequest req) {
        String motivo = (req == null) ? null : req.motivo();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.autorizar(idAutorizacion, idDocente, motivo));
    }

    @GetMapping("/{idAutorizacion}/intentos")
    public IntentosDto intentos(@PathVariable Long idAutorizacion, @RequestHeader(DOCENTE) Long idDocente) {
        return service.consultar(idAutorizacion, idDocente);
    }
}
