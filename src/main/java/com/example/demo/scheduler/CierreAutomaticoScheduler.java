package com.example.demo.scheduler;

import com.example.demo.repository.SesionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-19
 Para qué sirve: cierre AUTOMÁTICO de sesiones. Cada minuto llama a
 sp_cerrar_practicas_vencidas, que finaliza las sesiones EN_CURSO cuyo plazo
 ya pasó: cierra intentos, marca ausentes y califica.
 */
@Component
public class CierreAutomaticoScheduler {

    private static final Logger log = LoggerFactory.getLogger(CierreAutomaticoScheduler.class);

    private final SesionRepository repo;

    public CierreAutomaticoScheduler(SesionRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${soma.cierre.intervalo-ms:60000}")
    public void cerrarVencidas() {
        try {
            repo.cerrarVencidas();
        } catch (Exception e) {
            log.error("Error en el cierre automático de sesiones", e);
        }
    }
}
