package com.example.demo.repository;

import com.example.demo.dto.RegistroTiempoDto;
import com.example.demo.model.fila.Mapeo;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-12
 Para qué sirve: consulta los instantes que la BD ya registra solos: hora_inicio al iniciar la
 participación (trigger trg_participaciones_bi), hora_fin al finalizar (sp_finalizar_participacion)
 y el inicio/cierre real de la sesión (sp_iniciar_practica / sp_cerrar_practica).
 */
@Repository
public class RegistroTiempoRepository {

    private static final String SELECT = """
            SELECT pa.id_participacion, pa.numero_intento, a.id_estudiante, u.nombre_usuario,
                   pa.estado_participacion, pa.hora_inicio, pa.hora_fin,
                   TIMESTAMPDIFF(SECOND, pa.hora_inicio, pa.hora_fin) AS duracion,
                   pr.fecha_inicio_real, pr.fecha_cierre_real, pr.tipo_cierre
              FROM participaciones pa
              JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
              JOIN usuarios u ON u.id_usuario = a.id_estudiante
              JOIN practicas pr ON pr.id_practica = a.id_practica
              JOIN casos_estudio c ON c.id_caso = pr.id_caso
            """;

    private static final RowMapper<RegistroTiempoDto> MAPA = (rs, n) -> new RegistroTiempoDto(
            Mapeo.largo(rs, "id_participacion"), Mapeo.largo(rs, "numero_intento"),
            Mapeo.largo(rs, "id_estudiante"), Mapeo.texto(rs, "nombre_usuario"),
            Mapeo.texto(rs, "estado_participacion"), Mapeo.fecha(rs, "hora_inicio"),
            Mapeo.fecha(rs, "hora_fin"), Mapeo.largo(rs, "duracion"),
            Mapeo.fecha(rs, "fecha_inicio_real"), Mapeo.fecha(rs, "fecha_cierre_real"),
            Mapeo.texto(rs, "tipo_cierre"));

    private final JdbcTemplate jdbc;

    public RegistroTiempoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RegistroTiempoDto> porParticipacion(Long idParticipacion) {
        return jdbc.query(SELECT + " WHERE pa.id_participacion = ?", MAPA, idParticipacion)
                .stream().findFirst();
    }

    /** Todas las participaciones de una sesión; solo para el docente de la sesión o el creador del caso. */
    public List<RegistroTiempoDto> porSesion(Long idPractica, Long idDocente) {
        return jdbc.query(SELECT + " WHERE pr.id_practica = ? AND (pr.id_docente = ? OR c.id_docente_creador = ?) "
                + "ORDER BY u.nombre_usuario, pa.numero_intento", MAPA, idPractica, idDocente, idDocente);
    }
}
