package com.example.demo.repository;

import com.example.demo.dto.NotificacionDto;
import com.example.demo.model.fila.Mapeo;
import com.example.demo.notificacion.CorreoPendiente;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-20 || Base compartida (reutilizable en Módulo 4)
 Para qué sirve: (1) cola de correos PENDIENTES que envía el scheduler (la BD los encola al
 convocar, con el código dentro del contenido) y (2) bandeja de avisos de cada usuario.
 */
@Repository
public class NotificacionRepository {

    private final JdbcTemplate jdbc;

    public NotificacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----- cola de envío (RF-20) -----

    public List<CorreoPendiente> pendientesCorreo(int limite) {
        return jdbc.query("""
                SELECT n.id_notificacion, u.correoIns_usuario, n.asunto, n.contenido
                  FROM notificaciones n
                  JOIN usuarios u ON u.id_usuario = n.id_usuario
                 WHERE n.canal = 'CORREO' AND n.estado_envio = 'PENDIENTE'
                 ORDER BY n.id_notificacion
                 LIMIT ?
                """,
                (rs, n) -> new CorreoPendiente(rs.getLong("id_notificacion"),
                        rs.getString("correoIns_usuario"), rs.getString("asunto"),
                        rs.getString("contenido")),
                limite);
    }

    public void marcarEnviada(Long idNotificacion) {
        jdbc.update("UPDATE notificaciones SET estado_envio = 'ENVIADA', fecha_envio = NOW() "
                + "WHERE id_notificacion = ?", idNotificacion);
    }

    public void marcarFallida(Long idNotificacion) {
        jdbc.update("UPDATE notificaciones SET estado_envio = 'FALLIDA' WHERE id_notificacion = ?",
                idNotificacion);
    }

    // ----- bandeja del usuario (Base compartida) -----

    public List<NotificacionDto> listarPorUsuario(Long idUsuario, boolean soloNoLeidas) {
        return jdbc.query("""
                SELECT id_notificacion, id_practica, tipo_notificacion, canal, asunto, contenido,
                       estado_envio, fecha_creacion, leida
                  FROM notificaciones
                 WHERE id_usuario = ? AND (? = 0 OR leida = 0)
                 ORDER BY fecha_creacion DESC, id_notificacion DESC
                """,
                (rs, n) -> new NotificacionDto(Mapeo.largo(rs, "id_notificacion"), Mapeo.largo(rs, "id_practica"),
                        Mapeo.texto(rs, "tipo_notificacion"), Mapeo.texto(rs, "canal"),
                        Mapeo.texto(rs, "asunto"), Mapeo.texto(rs, "contenido"),
                        Mapeo.texto(rs, "estado_envio"), Mapeo.fecha(rs, "fecha_creacion"),
                        rs.getBoolean("leida")),
                idUsuario, soloNoLeidas ? 1 : 0);
    }

    /** Marca como leída solo si la notificación es de ese usuario. */
    public boolean marcarLeida(Long idNotificacion, Long idUsuario) {
        return jdbc.update("UPDATE notificaciones SET leida = 1, fecha_lectura = NOW() "
                + "WHERE id_notificacion = ? AND id_usuario = ?", idNotificacion, idUsuario) > 0;
    }
}
