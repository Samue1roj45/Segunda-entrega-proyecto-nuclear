package com.example.demo.model.fila;

import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `preguntas`. Preguntas de un escenario.
 */
public record PreguntaFila(Long idPregunta, Long idEscenario, Long idCriterio, Integer ordenPregunta, String enunciadoPregunta) {

    public static final RowMapper<PreguntaFila> MAPA = (rs, n) -> new PreguntaFila(
            Mapeo.largo(rs, "id_pregunta"),
            Mapeo.largo(rs, "id_escenario"),
            Mapeo.largo(rs, "id_criterio"),
            Mapeo.entero(rs, "orden_pregunta"),
            Mapeo.texto(rs, "enunciado_pregunta"));
}
