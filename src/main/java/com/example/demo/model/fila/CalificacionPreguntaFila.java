package com.example.demo.model.fila;

import java.math.BigDecimal;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `calificaciones_pregunta`. Puntaje por pregunta de un resultado.*/
public record CalificacionPreguntaFila(Long idResultado, Long idPregunta, BigDecimal puntajeObtenido) {

    public static final RowMapper<CalificacionPreguntaFila> MAPA = (rs, n) -> new CalificacionPreguntaFila(
            Mapeo.largo(rs, "id_resultado"),
            Mapeo.largo(rs, "id_pregunta"),
            Mapeo.decimal(rs, "puntaje_obtenido"));
}
