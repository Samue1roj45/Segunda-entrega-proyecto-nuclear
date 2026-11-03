package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

public record ParticipacionRespuesta(
        Long id,
        Long practicaId,
        String practicaNombre,
        Long estudianteId,
        String estudianteCorreo,
        String estudianteNombre,
        Long autorizacionId,
        int numeroIntento,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        int tiempoUsadoSeg,
        EstadoParticipacion estado) {
}
