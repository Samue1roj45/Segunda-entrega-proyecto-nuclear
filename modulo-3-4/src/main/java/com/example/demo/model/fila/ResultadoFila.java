package com.example.demo.model.fila;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `resultados`. Nota final del intento y retroalimentación general del docente.
 */
public record ResultadoFila(Long idResultado, Long idParticipacion, BigDecimal nota, LocalDateTime fechaCalculo, String feedbackDocente, Long idDocenteFeedback, LocalDateTime fechaFeedback) {

    public static final RowMapper<ResultadoFila> MAPA = (rs, n) -> new ResultadoFila(
            Mapeo.largo(rs, "id_resultado"),
            Mapeo.largo(rs, "id_participacion"),
            Mapeo.decimal(rs, "nota"),
            Mapeo.fecha(rs, "fecha_calculo"),
            Mapeo.texto(rs, "feedback_docente"),
            Mapeo.largo(rs, "id_docente_feedback"),
            Mapeo.fecha(rs, "fecha_feedback"));
}
