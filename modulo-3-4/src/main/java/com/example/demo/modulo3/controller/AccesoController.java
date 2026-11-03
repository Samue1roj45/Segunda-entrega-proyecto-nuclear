package com.example.demo.modulo3.controller;

import com.example.demo.dto.AccesoEstudianteRequest;
import com.example.demo.dto.AccesoEstudianteResponse;
import com.example.demo.service.AccesoEstudianteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-20
 Para qué sirve: punto de entrada del ESTUDIANTE. Se identifica con correo institucional
 + código de autorización (el que llegó por correo al ser convocado). Si es válido,
 devuelve idAutorizacion para iniciar su participación.
 */
@RestController
@RequestMapping("/api/acceso")
public class AccesoController {

    private final AccesoEstudianteService service;

    public AccesoController(AccesoEstudianteService service) {
        this.service = service;
    }

    @PostMapping("/estudiante")
    public AccesoEstudianteResponse accederEstudiante(@Valid @RequestBody AccesoEstudianteRequest req) {
        return service.autenticar(req.correo(), req.codigo());
    }
}
