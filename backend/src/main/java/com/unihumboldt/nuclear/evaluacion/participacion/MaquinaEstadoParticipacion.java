package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Patrón State: transiciones válidas de una participación (RF-12).
 */
public final class MaquinaEstadoParticipacion {

    private MaquinaEstadoParticipacion() {
    }

    public static Participacion iniciar(Participacion participacion, OffsetDateTime ahora) {
        if (participacion.getEstado() == EstadoParticipacion.EN_CURSO) {
            return participacion;
        }
        if (participacion.getEstado() != EstadoParticipacion.NO_INICIADA) {
            throw new TransicionInvalidaException(
                    "No es posible iniciar esta participación en su estado actual.");
        }
        participacion.setEstado(EstadoParticipacion.EN_CURSO);
        participacion.setInicio(ahora);
        participacion.setFin(null);
        participacion.setTiempoUsadoSeg(0);
        return participacion;
    }

    public static Participacion finalizar(Participacion participacion, OffsetDateTime ahora) {
        if (participacion.getEstado() == EstadoParticipacion.FINALIZADA) {
            return participacion;
        }
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO) {
            throw new TransicionInvalidaException(
                    "No es posible finalizar una participación que no está en curso.");
        }
        participacion.setEstado(EstadoParticipacion.FINALIZADA);
        participacion.setFin(ahora);
        if (participacion.getInicio() != null) {
            long segundos = Math.max(0, Duration.between(participacion.getInicio(), ahora).getSeconds());
            participacion.setTiempoUsadoSeg((int) segundos);
        }
        return participacion;
    }

    public static Participacion marcarIncompleta(Participacion participacion, OffsetDateTime ahora) {
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO) {
            throw new TransicionInvalidaException(
                    "Solo una participación en curso puede marcarse como incompleta.");
        }
        participacion.setEstado(EstadoParticipacion.INCOMPLETA);
        participacion.setFin(ahora);
        if (participacion.getInicio() != null) {
            long segundos = Math.max(0, Duration.between(participacion.getInicio(), ahora).getSeconds());
            participacion.setTiempoUsadoSeg((int) segundos);
        }
        return participacion;
    }
}
