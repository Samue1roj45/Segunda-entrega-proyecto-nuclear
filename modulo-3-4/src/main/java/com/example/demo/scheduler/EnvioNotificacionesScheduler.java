package com.example.demo.scheduler;

import com.example.demo.notificacion.CorreoPendiente;
import com.example.demo.notificacion.NotificadorCorreo;
import com.example.demo.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-20
 Para qué sirve: cada pocos segundos toma los correos PENDIENTES (convocatoria con el
 código de autorización, calificaciones, cambios) y los entrega con la estrategia
 configurada. Éxito -> ENVIADA; error -> FALLIDA (no bloquea a los demás).
 */
@Component
public class EnvioNotificacionesScheduler {

    private static final Logger log = LoggerFactory.getLogger(EnvioNotificacionesScheduler.class);

    private final NotificacionRepository repo;
    private final NotificadorCorreo notificador;

    public EnvioNotificacionesScheduler(NotificacionRepository repo, NotificadorCorreo notificador) {
        this.repo = repo;
        this.notificador = notificador;
    }

    @Scheduled(fixedDelayString = "${soma.notificaciones.intervalo-ms:15000}")
    public void enviarPendientes() {
        try {
            for (CorreoPendiente correo : repo.pendientesCorreo(50)) {
                try {
                    notificador.enviar(correo);
                    repo.marcarEnviada(correo.idNotificacion());
                } catch (Exception e) {
                    log.warn("No se pudo enviar la notificación {}: {}", correo.idNotificacion(), e.getMessage());
                    repo.marcarFallida(correo.idNotificacion());
                }
            }
        } catch (Exception e) {
            log.error("Error al leer la cola de notificaciones", e);
        }
    }
}
