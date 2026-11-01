package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `notificaciones`. Cola de correos y avisos del sistema.
 */
public record NotificacionFila(Long idNotificacion, Long idUsuario, Long idPractica, String tipoNotificacion, String canal, String asunto, String contenido, String estadoEnvio, LocalDateTime fechaCreacion, LocalDateTime fechaEnvio, Boolean leida, LocalDateTime fechaLectura) {

    public static final RowMapper<NotificacionFila> MAPA = (rs, n) -> new NotificacionFila(
            Mapeo.largo(rs, "id_notificacion"),
            Mapeo.largo(rs, "id_usuario"),
            Mapeo.largo(rs, "id_practica"),
            Mapeo.texto(rs, "tipo_notificacion"),
            Mapeo.texto(rs, "canal"),
            Mapeo.texto(rs, "asunto"),
            Mapeo.texto(rs, "contenido"),
            Mapeo.texto(rs, "estado_envio"),
            Mapeo.fecha(rs, "fecha_creacion"),
            Mapeo.fecha(rs, "fecha_envio"),
            Mapeo.bool(rs, "leida"),
            Mapeo.fecha(rs, "fecha_lectura"));
}
