package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * RF-12: un intento NO_INICIADA puede arrancar. No se cierra aquí:
 * la ausencia (nota 0) permanece NO_INICIADA y no usa esta transición.
 */
public final class EstadoNoIniciada implements EstadoDeParticipacion {

    static final EstadoNoIniciada INSTANCIA = new EstadoNoIniciada();

    private EstadoNoIniciada() {
    }

    @Override
    public EstadoParticipacion codigo() {
        return EstadoParticipacion.NO_INICIADA;
    }

    @Override
    public void iniciar(Participacion participacion, OffsetDateTime ahora) {
        participacion.setEstado(EstadoParticipacion.EN_CURSO);
        participacion.setInicio(ahora);
        participacion.setFin(null);
        participacion.setTiempoUsadoSeg(0);
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
