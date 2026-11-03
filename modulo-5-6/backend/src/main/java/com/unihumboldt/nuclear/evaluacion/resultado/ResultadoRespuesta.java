package com.unihumboldt.nuclear.evaluacion.resultado;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;

public record ResultadoRespuesta(
        Long id,
        Long participacionId,
        int numeroIntento,
        Long estudianteId,
        String estudianteCorreo,
        String estudianteNombre,
        Long practicaId,
        String practicaNombre,
        EstadoParticipacion estadoParticipacion,
        int correctas,
        int incorrectas,
        int noRespondidas,
        int pesoObtenido,
        int pesoTotal,
        int escalaMaxima,
        BigDecimal notaFinal,
        boolean oficial,
        BigDecimal notaOficial,
        OffsetDateTime fechaCalculo,
        List<DetallePreguntaResultado> detallePreguntas) {
}
