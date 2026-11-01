package com.example.demo.repository;

import com.example.demo.dto.ValidacionCasoDto;
import com.example.demo.model.fila.CasoEstudioFila;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: acceso a `casos_estudio` y a su ciclo BORRADOR -> PUBLICADO -> ARCHIVADO
 (RF-05/09/28). Publicar valida la estructura del caso en un trigger de la BD.*/
@Repository
public class CasoRepository {

    private final JdbcTemplate jdbc;

    public CasoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CasoEstudioFila> buscarPorId(Long idCaso) {
        return jdbc.query("SELECT * FROM casos_estudio WHERE id_caso = ?", CasoEstudioFila.MAPA, idCaso)
                .stream().findFirst();
    }

    /** Casos del docente; estado opcional (BORRADOR, PUBLICADO o ARCHIVADO). */
    public List<CasoEstudioFila> listarPorDocente(Long idDocente, String estado) {
        return jdbc.query("SELECT * FROM casos_estudio WHERE id_docente_creador = ? "
                + "AND (? IS NULL OR estado_caso = ?) ORDER BY fecha_creacion DESC",
                CasoEstudioFila.MAPA, idDocente, estado, estado);
    }

    public void publicar(Long idCaso, Long idDocente) {
        jdbc.update("CALL sp_publicar_caso(?,?)", idCaso, idDocente);
    }

    public void archivar(Long idCaso, Long idDocente) {
        jdbc.update("CALL sp_archivar_caso(?,?)", idCaso, idDocente);
    }

    /** sp_clonar_caso: copia el caso como un nuevo BORRADOR y devuelve su id. */
    public Long clonar(Long idCaso, Long idDocente) {
        return jdbc.queryForObject("CALL sp_clonar_caso(?,?)", Long.class, idCaso, idDocente);
    }

    /** sp_validar_caso: lista de verificaciones previas a publicar. */
    public List<ValidacionCasoDto> validar(Long idCaso) {
        return jdbc.query("CALL sp_validar_caso(?)",
                (rs, n) -> new ValidacionCasoDto(rs.getInt("paso"), rs.getString("verificacion"),
                        rs.getBoolean("cumple")),
                idCaso);
    }
}
