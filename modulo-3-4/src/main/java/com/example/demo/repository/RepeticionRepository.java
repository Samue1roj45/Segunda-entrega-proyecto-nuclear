package com.example.demo.repository;

import com.example.demo.model.DatosRepeticion;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-25
 Para qué sirve: acceso a la BD para las repeticiones. sp_autorizar_repeticion inserta la
 fila que concede UN intento extra; el trigger trg_repeticion_bi vuelve a validar que sea
 el docente creador del caso y que la sesión no esté finalizada.
*/
@Repository
public class RepeticionRepository {

    private final JdbcTemplate jdbc;

    public RepeticionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DatosRepeticion> cargar(Long idAutorizacion) {
        return jdbc.query("""
                SELECT a.estado AS estado_aut, c.id_docente_creador, pr.id_docente,
                       pr.estado_practica,
                       (SELECT COUNT(*) FROM participaciones p
                         WHERE p.id_autorizacion = a.id_autorizacion
                           AND p.estado_participacion <> 'AUSENTE') AS usados,
                       (SELECT COUNT(*) FROM autorizaciones_repeticion r
                         WHERE r.id_autorizacion = a.id_autorizacion) AS repeticiones
                  FROM autorizaciones a
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                  JOIN casos_estudio c ON c.id_caso = pr.id_caso
                 WHERE a.id_autorizacion = ?
                """,
                (rs, n) -> new DatosRepeticion(rs.getString("estado_aut"),
                        rs.getLong("id_docente_creador"), rs.getLong("id_docente"),
                        rs.getString("estado_practica"), rs.getInt("usados"),
                        rs.getInt("repeticiones")),
                idAutorizacion).stream().findFirst();
    }

    public void autorizar(Long idAutorizacion, Long idDocente, String motivo) {
        jdbc.update("CALL sp_autorizar_repeticion(?,?,?)", idAutorizacion, idDocente, motivo);
    }
}
