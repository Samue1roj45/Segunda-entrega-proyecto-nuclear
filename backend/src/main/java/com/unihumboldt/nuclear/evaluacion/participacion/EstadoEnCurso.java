package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * RF-12: el intento en curso puede reutilizarse al iniciar de nuevo,
 * finalizarse o marcarse incompleto (RF-24).
 */
public final class EstadoEnCurso implements EstadoDeParticipacion {

    static final EstadoEnCurso INSTANCIA = new EstadoEnCurso();

    private EstadoEnCurso() {
    }

    @Override
    public EstadoParticipacion codigo() {
        return EstadoParticipacion.EN_CURSO;
    }

    @Override
    public void iniciar(Participacion participacion, OffsetDateTime ahora) {
        // Reuso del mismo intento: no reinicia reloj ni cambia de estado.
    }

    @Override
    public void finalizar(Participacion participacion, OffsetDateTime ahora) {
        MaquinaEstadoParticipacion.cerrar(participacion, ahora, EstadoParticipacion.FINALIZADA);
    }

    @Override
    public void marcarIncompleta(Participacion participacion, OffsetDateTime ahora) {
        MaquinaEstadoParticipacion.cerrar(participacion, ahora, EstadoParticipacion.INCOMPLETA);
    }
}
