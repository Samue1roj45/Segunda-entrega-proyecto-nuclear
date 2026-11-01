package com.example.demo.modulo3.controller;

import com.example.demo.service.HerramientasService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-28
 Para qué sirve: entrega al estudiante las herramientas profesionales definidas para el caso
 (el JSON tal como lo definió la docente en el DSL). El uso de cada herramienta (calculadora,
 tablas, etc.) lo ejecuta el frontend con esa definición.
 */
@RestController
@RequestMapping("/api")
public class HerramientaController {

    private final HerramientasService service;

    public HerramientaController(HerramientasService service) {
        this.service = service;
    }

    @GetMapping(value = "/participaciones/{id}/herramientas", produces = MediaType.APPLICATION_JSON_VALUE)
    public String herramientas(@PathVariable Long id, @RequestHeader("X-Estudiante-Id") Long idEstudiante) {
        return service.consultar(idEstudiante, id);
    }
}
