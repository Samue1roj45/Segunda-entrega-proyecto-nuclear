package com.unihumboldt.nuclear.evaluacion.reporte;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;

/**
 * Una fila = un intento. No combina intentos.
 */
public record FilaReporteEvaluacion(
        Long participacionId,
        Long resultadoId,
        Long practicaId,
        String practicaNombre,
        Long estudianteId,
        String estudianteCorreo,
        String estudianteNombre,
        int numeroIntento,
        EstadoParticipacion estado,
        String clasificacion,
        Integer correctas,
        Integer incorrectas,
        Integer noRespondidas,
        Integer pesoObtenido,
        Integer pesoTotal,
        Integer escalaMaxima,
        BigDecimal notaFinal,
        boolean oficial,
        BigDecimal notaOficial,
        OffsetDateTime inicio,
        OffsetDateTime fin) {
}
