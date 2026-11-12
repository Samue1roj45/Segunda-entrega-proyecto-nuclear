package co.edu.humboldt.plataformacasos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Estado del backend único y de los módulos que contiene. Es público (no requiere token). */
@RestController
@RequestMapping("/api/salud")
public class SaludControlador {

    private static final List<Map<String, String>> MODULOS = List.of(
            Map.of("modulo", "1", "nombre", "Usuarios, autenticación y permisos", "requisitos", "RF-01, RF-02, RF-03"),
            Map.of("modulo", "2", "nombre", "Creación y validación de casos", "requisitos", "RF-05 a RF-09, RF-17"),
            Map.of("modulo", "3 y 4", "nombre", "Programación de sesiones y participación",
                    "requisitos", "RF-04, RF-10, RF-11, RF-18, RF-19, RF-20, RF-22, RF-23, RF-25, RF-28"),
            Map.of("modulo", "5 y 6", "nombre", "Evaluación, resultados, reportes y notificaciones",
                    "requisitos", "RF-12 a RF-16, RF-21, RF-24, RF-26, RF-27"));

    @GetMapping
    public Map<String, Object> estado() {
        return Map.of("estado", "UP", "aplicacion", "plataforma-casos", "modulos", MODULOS);
    }
}
