package co.edu.humboldt.plataformacasos.sesiones.repositorio;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Herramienta;
import co.edu.humboldt.plataformacasos.sesiones.dominio.TipoHerramienta;
import java.sql.PreparedStatement;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-28
 Para qué sirve: acceso a `herramientas_caso`, las herramientas profesionales que el docente creador
 definió para cada caso.
 */
@Repository
public class HerramientasRepositorio {

    private static final RowMapper<Herramienta> MAPA = (rs, n) -> new Herramienta(
            rs.getLong("id_herramienta"), rs.getLong("id_caso"), rs.getString("nombre"),
            TipoHerramienta.valueOf(rs.getString("tipo")), rs.getString("descripcion"),
            rs.getString("contenido"), rs.getInt("orden"));

    private final JdbcTemplate jdbc;

    public HerramientasRepositorio(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Herramienta> delCaso(Long idCaso) {
        return jdbc.query("SELECT * FROM herramientas_caso WHERE id_caso = ? ORDER BY orden, id_herramienta",
                MAPA, idCaso);
    }

    public Long registrar(Herramienta h) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO herramientas_caso (id_caso, nombre, tipo, descripcion, contenido, orden)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[] {"id_herramienta"});
            ps.setLong(1, h.idCaso());
            ps.setString(2, h.nombre());
            ps.setString(3, h.tipo().name());
            ps.setString(4, h.descripcion());
            ps.setString(5, h.contenido());
            ps.setInt(6, h.orden());
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    public boolean eliminar(Long idCaso, Long idHerramienta) {
        return jdbc.update("DELETE FROM herramientas_caso WHERE id_caso = ? AND id_herramienta = ?",
                idCaso, idHerramienta) > 0;
    }
}
