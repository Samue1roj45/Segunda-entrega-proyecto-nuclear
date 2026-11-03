package com.example.demo.modulo4.controller;

import com.example.demo.dto.RegistroTiempoDto;
import com.example.demo.service.RegistroTiempoService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-12
 Para qué sirve: API para consultar el registro de inicio y fin. El estudiante ve el de su
 participación; el docente ve el de todos los participantes de su sesión.
 */
@RestController
@RequestMapping("/api")
public class RegistroTiempoController {

    private final RegistroTiempoService service;

    public RegistroTiempoController(RegistroTiempoService service) {
        this.service = service;
    }

    @GetMapping("/participaciones/{id}/registro")
    public RegistroTiempoDto registro(@PathVariable Long id, @RequestHeader("X-Estudiante-Id") Long idEstudiante) {
        return service.consultarPropio(idEstudiante, id);
    }

    @GetMapping("/sesiones/{id}/registros")
    public List<RegistroTiempoDto> registrosSesion(@PathVariable Long id, @RequestHeader("X-Docente-Id") Long idDocente) {
        return service.listarSesion(id, idDocente);
    }
}
