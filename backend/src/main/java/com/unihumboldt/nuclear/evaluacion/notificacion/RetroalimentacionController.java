package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/retroalimentaciones")
public class RetroalimentacionController {

    private final GestionRetroalimentacion gestion;

    public RetroalimentacionController(GestionRetroalimentacion gestion) {
        this.gestion = gestion;
    }

    @PostMapping
    public RetroalimentacionRespuesta registrar(@Valid @RequestBody RegistrarRetroalimentacionRequest request) {
        return gestion.registrar(request);
    }

    @GetMapping
    public List<RetroalimentacionRespuesta> listar(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long participacion) {
        return gestion.listar(practica, participacion);
    }

    @GetMapping("/{id}")
    public RetroalimentacionRespuesta obtener(@PathVariable Long id) {
        return gestion.obtener(id);
    }
}
