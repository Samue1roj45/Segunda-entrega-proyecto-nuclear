package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Contexto del State: elige el estado concreto según el enum persistido (RF-12).
 */
public final class MaquinaEstadoParticipacion {

    private MaquinaEstadoParticipacion() {
    }

    public static EstadoDeParticipacion de(Participacion participacion) {
        EstadoParticipacion codigo = participacion.getEstado();
        if (codigo == null) {
            throw new TransicionInvalidaException("La participación no tiene estado.");
        }
        return switch (codigo) {
            case NO_INICIADA -> EstadoNoIniciada.INSTANCIA;
            case EN_CURSO -> EstadoEnCurso.INSTANCIA;
            case FINALIZADA -> EstadoFinalizada.INSTANCIA;
            case INCOMPLETA -> EstadoIncompleta.INSTANCIA;
        };
    }

    public static Participacion iniciar(Participacion participacion, OffsetDateTime ahora) {
        de(participacion).iniciar(participacion, ahora);
        return participacion;
    }

    public static Participacion finalizar(Participacion participacion, OffsetDateTime ahora) {
        de(participacion).finalizar(participacion, ahora);
        return participacion;
    }

    public static Participacion marcarIncompleta(Participacion participacion, OffsetDateTime ahora) {
        de(participacion).marcarIncompleta(participacion, ahora);
        return participacion;
    }

    static void cerrar(Participacion participacion, OffsetDateTime ahora, EstadoParticipacion destino) {
        participacion.setEstado(destino);
        participacion.setFin(ahora);
        if (participacion.getInicio() != null) {
            long segundos = Math.max(0, Duration.between(participacion.getInicio(), ahora).getSeconds());
            participacion.setTiempoUsadoSeg((int) segundos);
        }
    }
}
