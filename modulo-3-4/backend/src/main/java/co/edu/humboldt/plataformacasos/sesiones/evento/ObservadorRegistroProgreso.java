package co.edu.humboldt.plataformacasos.sesiones.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
/*
 #REQUISITO RF-11 (trazabilidad)
 Patrón: OBSERVER (observador concreto)
 Para qué sirve: deja traza del progreso cada vez que se guarda una respuesta.
 Corre DESPUÉS del commit: solo registra respuestas realmente guardadas.
 Nuevos observadores (alertas al docente, métricas, SSE al frontend) se agregan como
 otra clase @Component sin tocar ServicioParticipacionEstudianteImpl.
 */
@Component
public class ObservadorRegistroProgreso {

    private static final Logger log = LoggerFactory.getLogger(ObservadorRegistroProgreso.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarRespuesta(RespuestaRegistradaEvento e) {
        log.info("Participación {}: pregunta {} -> opción {} | progreso {}/{} ({}%)",
                e.idParticipacion(), e.idPregunta(), e.idRespuesta(),
                e.progreso().preguntasRespondidas(), e.progreso().totalPreguntas(),
                e.progreso().porcentajeProgreso());
    }
}
