package com.example.demo.scheduler;

import com.example.demo.repository.ParticipacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-23
 Para qué sirve: cuando a un estudiante se le agota el tiempo máximo, su intento se cierra
 SOLO: cada 30 s llama a sp_expirar_participaciones, que finaliza los intentos vencidos
 (INCOMPLETA si faltó responder), los califica y encola la notificación.
 */
@Component
public class ExpiracionParticipacionesScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionParticipacionesScheduler.class);

    private final ParticipacionRepository repo;

    public ExpiracionParticipacionesScheduler(ParticipacionRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${soma.expiracion.intervalo-ms:30000}")
    public void expirarVencidas() {
        try {
            repo.expirarVencidas();
        } catch (Exception e) {
            log.error("Error al expirar participaciones vencidas", e);
        }
    }
}
