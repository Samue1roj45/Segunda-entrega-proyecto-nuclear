package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * RF-12 / RF-14: un intento FINALIZADA no se reabre. Finalizar de nuevo es idempotente.
 */
public final class EstadoFinalizada implements EstadoDeParticipacion {

    static final EstadoFinalizada INSTANCIA = new EstadoFinalizada();

    private EstadoFinalizada() {
    }

    @Override
    public EstadoParticipacion codigo() {
        return EstadoParticipacion.FINALIZADA;
    }

    @Override
    public void iniciar(Participacion participacion, OffsetDateTime ahora) {
        throw new TransicionInvalidaException(
                "No es posible iniciar esta participación en su estado actual.");
    }

    @Override
    public void finalizar(Participacion participacion, OffsetDateTime ahora) {
        // Idempotente: el intento ya está cerrado.
    }

    @Override
    public void marcarIncompleta(Participacion participacion, OffsetDateTime ahora) {
        throw new TransicionInvalidaException(
                "Solo una participación en curso puede marcarse como incompleta.");
    }
}
