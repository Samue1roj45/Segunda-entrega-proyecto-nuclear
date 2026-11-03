package com.unihumboldt.nuclear.evaluacion.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "modulo", "evaluacion-resultados",
                "alcance", "RF-12,RF-13,RF-14,RF-15,RF-16,RF-21,RF-24,RF-26,RF-27");
    }
}
