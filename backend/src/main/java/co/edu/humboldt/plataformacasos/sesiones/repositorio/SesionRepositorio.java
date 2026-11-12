package co.edu.humboldt.plataformacasos.sesiones.repositorio;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.estado.AccionesSesion;
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
 #REQUISITO RF-04 || RF-18 || RF-19
 Para qué sirve: acceso a la tabla `practicas` (las sesiones): agendar, consultar, cambiar condiciones
 y cambiar de estado. Aquí NO hay reglas de negocio: viven en la cadena de validación (Chain), los
 estados (State) y los servicios. Implementa AccionesSesion para que los estados no conozcan JDBC.
 */
@Repository
public class SesionRepositorio implements AccionesSesion {

    private static final String SELECT = """
            SELECT id_practica, id_caso, id_docente, espacio_academico, fecha_inicio, fecha_fin,
                   tiempo_maximo_minutos, porcentaje_limite_ingreso_tardio, lugar, mensaje_personalizado,
                   url_acceso, estado_practica, fecha_inicio_real, fecha_cierre_real, tipo_cierre
              FROM practicas
            """;

    private static final RowMapper<Practica> MAPA = (rs, n) -> new Practica(
            rs.getLong("id_practica"), rs.getLong("id_caso"), rs.getLong("id_docente"),
            rs.getString("espacio_academico"),
            rs.getObject("fecha_inicio", LocalDateTime.class),
            rs.getObject("fecha_fin", LocalDateTime.class),
            rs.getInt("tiempo_maximo_minutos"), rs.getBigDecimal("porcentaje_limite_ingreso_tardio"),
            rs.getString("lugar"), rs.getString("mensaje_personalizado"), rs.getString("url_acceso"),
            rs.getString("estado_practica"),
            rs.getObject("fecha_inicio_real", LocalDateTime.class),
            rs.getObject("fecha_cierre_real", LocalDateTime.class),
            rs.getString("tipo_cierre"));

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    public SesionRepositorio(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    // ---------- RF-04 / RF-18: agendar y condiciones ----------

    /** Crea la sesión en estado SIN_INICIAR y devuelve su id. */
    public Long agendar(SesionProgramada s) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO practicas (id_caso, id_docente, espacio_academico, fecha_inicio, fecha_fin,
                                           tiempo_maximo_minutos, lugar, mensaje_personalizado, url_acceso,
                                           estado_practica, fecha_creacion)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SIN_INICIAR', ?)
                    """, new String[] {"id_practica"});
            ps.setLong(1, s.idCaso());
            ps.setLong(2, s.idDocente());
            ps.setString(3, s.espacioAcademico().trim());
            ps.setTimestamp(4, Timestamp.valueOf(s.fechaInicio()));
            ps.setTimestamp(5, Timestamp.valueOf(s.fechaFin()));
            ps.setInt(6, s.tiempoMaximoMinutos());
            ps.setString(7, s.lugar());
            ps.setString(8, s.mensajePersonalizado());
            ps.setString(9, s.urlAcceso());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now(reloj)));
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    /** Cambia las condiciones (RF-18) de una sesión del docente que aún no ha finalizado. */
    public boolean actualizarCondiciones(Long idPractica, SesionProgramada s) {
        return jdbc.update("""
                UPDATE practicas
                   SET espacio_academico = ?, fecha_inicio = ?, fecha_fin = ?, tiempo_maximo_minutos = ?,
                       lugar = ?, mensaje_personalizado = ?, url_acceso = ?
                 WHERE id_practica = ? AND id_docente = ? AND estado_practica <> 'FINALIZADO'
                """, s.espacioAcademico().trim(), Timestamp.valueOf(s.fechaInicio()),
                Timestamp.valueOf(s.fechaFin()), s.tiempoMaximoMinutos(), s.lugar(), s.mensajePersonalizado(),
                s.urlAcceso(), idPractica, s.idDocente()) > 0;
    }

    // ---------- RF-18: consultar sesiones ----------

    public Optional<Practica> buscar(Long idPractica) {
        return jdbc.query(SELECT + " WHERE id_practica = ?", MAPA, idPractica).stream().findFirst();
    }

    /** La sesión solo existe para el docente que la programó. */
    public Optional<Practica> buscar(Long idPractica, Long idDocente) {
        return jdbc.query(SELECT + " WHERE id_practica = ? AND id_docente = ?", MAPA, idPractica, idDocente)
                .stream().findFirst();
    }

    /** Sesiones del docente, de la más reciente a la más antigua; estado opcional. */
    public List<Practica> listarPorDocente(Long idDocente, String estado) {
        if (estado == null) {
            return jdbc.query(SELECT + " WHERE id_docente = ? ORDER BY fecha_inicio DESC, id_practica DESC",
                    MAPA, idDocente);
        }
        return jdbc.query(SELECT + " WHERE id_docente = ? AND estado_practica = ?"
                + " ORDER BY fecha_inicio DESC, id_practica DESC", MAPA, idDocente, estado);
    }

    /** RF-19: sesiones EN_CURSO cuyo plazo ya terminó (para el cierre automático). */
    public List<Practica> vencidasEnCurso() {
        return jdbc.query(SELECT + " WHERE estado_practica = 'EN_CURSO' AND fecha_fin < ?", MAPA,
                Timestamp.valueOf(LocalDateTime.now(reloj)));
    }

    // ---------- RF-19: estados (los invocan los objetos State) ----------

    /** SIN_INICIAR -> EN_CURSO, guardando el inicio real. */
    @Override
    public void iniciarEnBd(Long idPractica, Long idDocente) {
        jdbc.update("""
                UPDATE practicas SET estado_practica = 'EN_CURSO', fecha_inicio_real = ?
                 WHERE id_practica = ? AND id_docente = ? AND estado_practica = 'SIN_INICIAR'
                """, Timestamp.valueOf(LocalDateTime.now(reloj)), idPractica, idDocente);
    }

    /** EN_CURSO -> FINALIZADO por decisión del docente (cierre MANUAL). */
    @Override
    public void finalizarEnBd(Long idPractica, Long idDocente) {
        cerrar(idPractica, idDocente, "MANUAL");
    }

    /** EN_CURSO -> FINALIZADO porque terminó su plazo (cierre AUTOMATICO, RF-19). */
    public boolean cerrarAutomaticamente(Long idPractica) {
        return jdbc.update("""
                UPDATE practicas SET estado_practica = 'FINALIZADO', fecha_cierre_real = ?, tipo_cierre = 'AUTOMATICO'
                 WHERE id_practica = ? AND estado_practica = 'EN_CURSO'
                """, Timestamp.valueOf(LocalDateTime.now(reloj)), idPractica) > 0;
    }

    private void cerrar(Long idPractica, Long idDocente, String tipo) {
        jdbc.update("""
                UPDATE practicas SET estado_practica = 'FINALIZADO', fecha_cierre_real = ?, tipo_cierre = ?
                 WHERE id_practica = ? AND id_docente = ? AND estado_practica = 'EN_CURSO'
                """, Timestamp.valueOf(LocalDateTime.now(reloj)), tipo, idPractica, idDocente);
    }
}
