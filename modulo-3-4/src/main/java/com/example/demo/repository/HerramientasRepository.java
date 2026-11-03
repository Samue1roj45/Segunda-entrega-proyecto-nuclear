package com.example.demo.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-28
 Para qué sirve: lee las herramientas profesionales definidas para el caso dentro del documento
 DSL (versiones_dsl_caso.documento_json). Toma la versión VÁLIDA más reciente del caso y extrae
 solo el nodo de herramientas con JSON_EXTRACT (la ruta es configurable).
 */
@Repository
public class HerramientasRepository {

    private final JdbcTemplate jdbc;

    public HerramientasRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> herramientasDelCaso(Long idParticipacion, String rutaJson) {
        return jdbc.query("""
                SELECT COALESCE(JSON_EXTRACT(v.documento_json, ?), JSON_ARRAY()) AS herramientas
                  FROM participaciones pa
                  JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                  JOIN versiones_dsl_caso v ON v.id_caso = pr.id_caso AND v.estado_validacion = 'VALIDO'
                 WHERE pa.id_participacion = ?
                 ORDER BY v.fecha_creacion DESC, v.id_version_dsl DESC
                 LIMIT 1
                """,
                (rs, n) -> rs.getString("herramientas"), rutaJson, idParticipacion)
                .stream().findFirst();
    }

    public String estadoParticipacion(Long idParticipacion) {
        return jdbc.queryForObject("SELECT estado_participacion FROM participaciones WHERE id_participacion = ?",
                String.class, idParticipacion);
    }
}
