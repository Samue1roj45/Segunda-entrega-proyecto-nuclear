package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.time.OffsetDateTime;

// Patrón State: contrato de los estados de un intento (NO_INICIADA, EN_CURSO, FINALIZADA, INCOMPLETA).
/**
 * State (GoF) de un intento: cada estado concreto decide qué transiciones admite.
 */
public interface EstadoDeParticipacion {

    EstadoParticipacion codigo();

    void iniciar(Participacion participacion, OffsetDateTime ahora);

    void finalizar(Participacion participacion, OffsetDateTime ahora);

    void marcarIncompleta(Participacion participacion, OffsetDateTime ahora);
}
