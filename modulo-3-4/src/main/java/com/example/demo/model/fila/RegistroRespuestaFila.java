package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `registros_respuesta`. Opción elegida por pregunta (una fila por pregunta).
 */
public record RegistroRespuestaFila(Long idRegistro, Long idParticipacion, Long idPregunta, Long idRespuesta, LocalDateTime fechaRespuesta) {

    public static final RowMapper<RegistroRespuestaFila> MAPA = (rs, n) -> new RegistroRespuestaFila(
            Mapeo.largo(rs, "id_registro"),
            Mapeo.largo(rs, "id_participacion"),
            Mapeo.largo(rs, "id_pregunta"),
            Mapeo.largo(rs, "id_respuesta"),
            Mapeo.fecha(rs, "fecha_respuesta"));
}
