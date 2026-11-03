package com.unihumboldt.nuclear.evaluacion.resultado;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record NotaOficialRespuesta(
        Long id,
        Long practicaId,
        Long estudianteId,
        Long participacionId,
        int numeroIntento,
        BigDecimal notaOficial,
        Long docenteId,
        OffsetDateTime fechaSeleccion) {
}
