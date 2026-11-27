package co.edu.humboldt.plataformacasos.sesiones.repositorio;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-04 || RF-20
 Para qué sirve: acceso a la tabla `autorizaciones`: los estudiantes convocados a cada sesión y el
 hash de su código de acceso. Retirar a un estudiante no borra la fila (queda RETIRADA).
 */
@Repository
public class AutorizacionRepositorio {

    private static final String SELECT = """
            SELECT id_autorizacion, id_practica, id_estudiante, codigo_hash, estado, fecha_creacion
              FROM autorizaciones
            """;

    private static final RowMapper<Autorizacion> MAPA = (rs, n) -> new Autorizacion(
            rs.getLong("id_autorizacion"), rs.getLong("id_practica"), rs.getLong("id_estudiante"),
            rs.getString("codigo_hash"), rs.getString("estado"),
            rs.getObject("fecha_creacion", LocalDateTime.class));

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public AutorizacionRepositorio(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public Long insertar(Long idPractica, Long idEstudiante, String codigoHash) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO autorizaciones (id_practica, id_estudiante, codigo_hash, estado, fecha_creacion)
                    VALUES (?, ?, ?, 'ACTIVA', ?)
                    """, new String[] {"id_autorizacion"});
            ps.setLong(1, idPractica);
            ps.setLong(2, idEstudiante);
            ps.setString(3, codigoHash);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now(reloj)));
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    /** Vuelve a convocar a un estudiante retirado, con un código nuevo. */
    public void reactivar(Long idAutorizacion, String codigoHash) {
        jdbc.update("UPDATE autorizaciones SET estado = 'ACTIVA', codigo_hash = ?, fecha_retiro = NULL "
                + "WHERE id_autorizacion = ?", codigoHash, idAutorizacion);
    }

    public boolean retirar(Long idPractica, Long idEstudiante) {
        return jdbc.update("UPDATE autorizaciones SET estado = 'RETIRADA', fecha_retiro = ? "
                        + "WHERE id_practica = ? AND id_estudiante = ? AND estado = 'ACTIVA'",
                Timestamp.valueOf(LocalDateTime.now(reloj)), idPractica, idEstudiante) > 0;
    }

    public Optional<Autorizacion> buscar(Long idAutorizacion) {
        return jdbc.query(SELECT + " WHERE id_autorizacion = ?", MAPA, idAutorizacion).stream().findFirst();
    }

    public Optional<Autorizacion> buscar(Long idPractica, Long idEstudiante) {
        return jdbc.query(SELECT + " WHERE id_practica = ? AND id_estudiante = ?", MAPA, idPractica, idEstudiante)
                .stream().findFirst();
    }

    /** Todos los convocados de la sesión (activos y retirados), en orden de convocatoria. */
    public List<Autorizacion> dePractica(Long idPractica) {
        return jdbc.query(SELECT + " WHERE id_practica = ? ORDER BY id_autorizacion", MAPA, idPractica);
    }

    public List<Autorizacion> activasDePractica(Long idPractica) {
        return jdbc.query(SELECT + " WHERE id_practica = ? AND estado = 'ACTIVA' ORDER BY id_autorizacion",
                MAPA, idPractica);
    }

    /** Autorizaciones activas del estudiante, de la sesión más reciente a la más antigua. */
    public List<Autorizacion> activasDeEstudiante(Long idEstudiante) {
        return jdbc.query(SELECT + " WHERE id_estudiante = ? AND estado = 'ACTIVA' ORDER BY id_autorizacion DESC",
                MAPA, idEstudiante);
    }

    /**
     * Hashes de los códigos con los que el estudiante puede entrar a la plataforma. El código es su contraseña: sirve
     * siempre, sin importar si lo retiraron de una sesión o si la sesión ya finalizó. Retirarlo lo saca de ESA sesión
     * (no verá ese caso), nunca de la plataforma.
     */
    public List<String> hashesDeEstudiante(Long idEstudiante) {
        return jdbc.queryForList("SELECT codigo_hash FROM autorizaciones WHERE id_estudiante = ?", String.class, idEstudiante);
    }

    /** Hash de la contraseña propia del estudiante (la que crea al aceptar una invitación), si la tiene. */
    public Optional<String> claveHashDeEstudiante(Long idEstudiante) {
        return jdbc.queryForList("SELECT clave_hash FROM usuario WHERE id = ? AND clave_hash IS NOT NULL", String.class, idEstudiante)
                .stream().findFirst();
    }

    /** RF-25: repeticiones excepcionales concedidas a la autorización (tabla autorizaciones_repeticion). */
    public int contarRepeticiones(Long idAutorizacion) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones_repeticion WHERE id_autorizacion = ?",
                Integer.class, idAutorizacion);
        return n == null ? 0 : n;
    }
}
