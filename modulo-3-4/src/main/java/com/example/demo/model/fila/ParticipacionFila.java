package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `participaciones`. Intento de un estudiante autorizado.
 */
public record ParticipacionFila(Long idParticipacion, Long idAutorizacion, Long numeroIntento, LocalDateTime horaInicio, LocalDateTime horaFin, String estadoParticipacion) {

    public static final RowMapper<ParticipacionFila> MAPA = (rs, n) -> new ParticipacionFila(
            Mapeo.largo(rs, "id_participacion"),
            Mapeo.largo(rs, "id_autorizacion"),
            Mapeo.largo(rs, "numero_intento"),
            Mapeo.fecha(rs, "hora_inicio"),
            Mapeo.fecha(rs, "hora_fin"),
            Mapeo.texto(rs, "estado_participacion"));
}
