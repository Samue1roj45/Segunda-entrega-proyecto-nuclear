package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * RF-24: INCOMPLETA es terminal para ese intento. Un segundo intento es otra participación.
 */
public final class EstadoIncompleta implements EstadoDeParticipacion {

    static final EstadoIncompleta INSTANCIA = new EstadoIncompleta();

    private EstadoIncompleta() {
    }

    @Override
    public EstadoParticipacion codigo() {
        return EstadoParticipacion.INCOMPLETA;
    }

    @Override
    public void iniciar(Participacion participacion, OffsetDateTime ahora) {
        throw new TransicionInvalidaException(
                "No es posible iniciar esta participación en su estado actual.");
    }

    @Override
    public void finalizar(Participacion participacion, OffsetDateTime ahora) {
        throw new TransicionInvalidaException(
                "No es posible finalizar una participación que no está en curso.");
    }

    @Override
    public void marcarIncompleta(Participacion participacion, OffsetDateTime ahora) {
        throw new TransicionInvalidaException(
                "Solo una participación en curso puede marcarse como incompleta.");
    }
}
