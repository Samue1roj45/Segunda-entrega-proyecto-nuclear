package com.example.demo.repository;

import com.example.demo.dto.AccesoEstudianteResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-20
 Para qué sirve: valida en la BD el acceso del estudiante (correo + código). El
 procedimiento comprueba usuario activo con rol ESTUDIANTE, código vigente y sesión no
 finalizada; si algo falla lanza SIGNAL 45000.
 */
@Repository
public class AccesoRepository {

    private final JdbcTemplate jdbc;

    public AccesoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AccesoEstudianteResponse autenticarEstudiante(String correo, String codigo) {
        return jdbc.queryForObject("CALL sp_autenticar_estudiante(?,?)",
                (rs, n) -> new AccesoEstudianteResponse(rs.getLong("id_autorizacion"),
                        rs.getLong("id_practica"), rs.getLong("id_usuario"),
                        rs.getString("nombre_usuario"), rs.getString("nombre_caso"),
                        rs.getString("estado_sesion")),
                correo, codigo);
    }
}
