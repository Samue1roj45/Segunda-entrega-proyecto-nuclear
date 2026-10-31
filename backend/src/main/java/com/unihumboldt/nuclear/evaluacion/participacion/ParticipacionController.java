package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/participaciones")
public class ParticipacionController {

    private final ParticipacionFacade facade;

    public ParticipacionController(ParticipacionFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<ParticipacionRespuesta> listar(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante,
            @RequestParam(required = false) EstadoParticipacion estado,
            @RequestParam(required = false) Integer numeroIntento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return facade.listar(practica, estudiante, estado, numeroIntento, fechaDesde, fechaHasta);
    }

    @PostMapping("/iniciar")
    public ParticipacionRespuesta iniciar(@Valid @RequestBody IniciarParticipacionRequest request) {
        return facade.iniciar(request);
    }

    @GetMapping("/{id}")
    public ParticipacionRespuesta obtener(@PathVariable Long id) {
        return facade.obtener(id);
    }

    @PostMapping("/{id}/respuestas")
    public void responder(@PathVariable Long id, @Valid @RequestBody RegistrarRespuestaRequest request) {
        facade.responder(id, request);
    }

    @PostMapping("/{id}/finalizar")
    public ParticipacionRespuesta finalizar(@PathVariable Long id) {
        return facade.finalizar(id);
    }
}
