package co.edu.humboldt.plataformacasos.sesiones.tareas;

import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-23
 Para qué sirve: cuando a un estudiante se le agota el tiempo máximo, su intento se cierra
 SOLO: cada 30 s cierra los intentos vencidos
 (INCOMPLETA si faltó responder) y el módulo 5 los califica.
 */
@Component
public class TareaExpiracionParticipaciones {

    private static final Logger log = LoggerFactory.getLogger(TareaExpiracionParticipaciones.class);

    private final ParticipacionEstudianteRepositorio repo;

    public TareaExpiracionParticipaciones(ParticipacionEstudianteRepositorio repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${app.sesiones.intervalo-expiracion-ms:30000}")
    public void expirarVencidas() {
        try {
            repo.expirarVencidas();
        } catch (Exception e) {
            log.error("Error al expirar participaciones vencidas", e);
        }
    }
}
