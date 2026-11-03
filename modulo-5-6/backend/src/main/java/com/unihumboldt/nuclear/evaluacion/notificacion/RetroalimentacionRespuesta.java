package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.OffsetDateTime;

public record RetroalimentacionRespuesta(
        Long id,
        Long participacionId,
        int numeroIntento,
        Long estudianteId,
        Long practicaId,
        Long docenteId,
        String texto,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion) {
}
