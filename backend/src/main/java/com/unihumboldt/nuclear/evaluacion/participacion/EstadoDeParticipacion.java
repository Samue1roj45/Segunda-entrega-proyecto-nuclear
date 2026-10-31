package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * State (GoF) de un intento: cada estado concreto decide qué transiciones admite.
 */
public interface EstadoDeParticipacion {

    EstadoParticipacion codigo();

    void iniciar(Participacion participacion, OffsetDateTime ahora);

    void finalizar(Participacion participacion, OffsetDateTime ahora);

    void marcarIncompleta(Participacion participacion, OffsetDateTime ahora);
}
