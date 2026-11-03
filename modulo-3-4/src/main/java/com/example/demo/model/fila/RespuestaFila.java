package com.example.demo.model.fila;

import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `respuestas`. OJO: esCorrecta y retroalimentacion NUNCA deben viajar al estudiante durante su participación.
 */
public record RespuestaFila(Long idRespuesta, Long idPregunta, Integer ordenRespuesta, String textoRespuesta, Boolean esCorrecta, String retroalimentacion) {

    public static final RowMapper<RespuestaFila> MAPA = (rs, n) -> new RespuestaFila(
            Mapeo.largo(rs, "id_respuesta"),
            Mapeo.largo(rs, "id_pregunta"),
            Mapeo.entero(rs, "orden_respuesta"),
            Mapeo.texto(rs, "texto_respuesta"),
            Mapeo.bool(rs, "es_correcta"),
            Mapeo.texto(rs, "retroalimentacion"));
}
