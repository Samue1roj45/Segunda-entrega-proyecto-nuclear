package com.example.demo.repository;

import com.example.demo.dto.ConvocadoDto;
import com.example.demo.dto.SesionDto;
import com.example.demo.model.SesionProgramada;
import com.example.demo.state.AccionesSesion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Para qué sirve: acceso a la BD para programar sesiones, asociar estudiantes, consultar
 sesiones y cambiar su estado. Cada método llama a un procedimiento almacenado; aquí NO
 hay reglas de negocio (viven en la BD: triggers y sp_*).
 */
@Repository
public class SesionRepository implements AccionesSesion {

    private static final String SELECT_SESION = """
            SELECT pr.id_practica, pr.id_caso, c.nombre_caso, pr.espacio_academico,
                   pr.fecha_inicio, pr.fecha_fin, pr.tiempo_maximo_minutos, pr.lugar,
                   pr.mensaje_personalizado, pr.url_acceso, pr.estado_practica,
                   pr.fecha_inicio_real, pr.fecha_cierre_real, pr.tipo_cierre
              FROM practicas pr
              JOIN casos_estudio c ON c.id_caso = pr.id_caso
            """;

    private static final RowMapper<SesionDto> MAPA_SESION = (rs, n) -> new SesionDto(
            rs.getLong("id_practica"), rs.getLong("id_caso"), rs.getString("nombre_caso"),
            rs.getString("espacio_academico"),
            rs.getObject("fecha_inicio", LocalDateTime.class),
            rs.getObject("fecha_fin", LocalDateTime.class),
            rs.getInt("tiempo_maximo_minutos"), rs.getString("lugar"),
            rs.getString("mensaje_personalizado"), rs.getString("url_acceso"),
            rs.getString("estado_practica"),
            rs.getObject("fecha_inicio_real", LocalDateTime.class),
            rs.getObject("fecha_cierre_real", LocalDateTime.class),
            rs.getString("tipo_cierre"), List.of());

    private final JdbcTemplate jdbc;

    public SesionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- RF-04 / RF-18: agendar y estudiantes ----------

    /** sp_agendar_practica: crea la sesión y devuelve su id. */
    public Long agendar(SesionProgramada s) {
        return jdbc.queryForObject("CALL sp_agendar_practica(?,?,?,?,?,?,?,?,?)", Long.class,
                s.idCaso(), s.idDocente(), s.espacioAcademico(), s.fechaInicio(), s.fechaFin(),
                s.tiempoMaximoMinutos(), s.lugar(), s.mensajePersonalizado(), s.urlAcceso());
    }

    /** sp_agregar_estudiante: crea la autorización con su código y encola la notificación. */
    public Long agregarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        return jdbc.queryForObject("CALL sp_agregar_estudiante(?,?,?)", Long.class,
                idPractica, idDocente, idEstudiante);
    }

    /** sp_retirar_estudiante: marca la autorización como RETIRADA (conserva historial). */
    public void retirarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        jdbc.update("CALL sp_retirar_estudiante(?,?,?)", idPractica, idDocente, idEstudiante);
    }

    /** Lista los estudiantes convocados a una sesión del docente. */
    public List<ConvocadoDto> listarConvocados(Long idPractica, Long idDocente) {
        return jdbc.query("""
                SELECT a.id_autorizacion, a.id_estudiante, u.nombre_usuario,
                       u.correoIns_usuario, a.estado
                  FROM autorizaciones a
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                  JOIN usuarios u ON u.id_usuario = a.id_estudiante
                 WHERE a.id_practica = ? AND pr.id_docente = ?
                 ORDER BY u.nombre_usuario
                """,
                (rs, n) -> new ConvocadoDto(rs.getLong("id_autorizacion"),
                        rs.getLong("id_estudiante"), rs.getString("nombre_usuario"),
                        rs.getString("correoIns_usuario"), rs.getString("estado")),
                idPractica, idDocente);
    }

    // ---------- RF-18: consultar sesiones ----------

    /** Sesiones del docente, de la más reciente a la más antigua; estado opcional. */
    public List<SesionDto> listarPorDocente(Long idDocente, String estado) {
        return jdbc.query(SELECT_SESION
                        + " WHERE pr.id_docente = ? AND (? IS NULL OR pr.estado_practica = ?)"
                        + " ORDER BY pr.fecha_inicio DESC",
                MAPA_SESION, idDocente, estado, estado);
    }

    public Optional<SesionDto> buscar(Long idPractica, Long idDocente) {
        return jdbc.query(SELECT_SESION + " WHERE pr.id_practica = ? AND pr.id_docente = ?",
                MAPA_SESION, idPractica, idDocente).stream().findFirst();
    }

    // ---------- RF-19: estados (los invocan los objetos State) ----------

    /** sp_iniciar_practica: SIN_INICIAR -> EN_CURSO. */
    @Override
    public void iniciarEnBd(Long idPractica, Long idDocente) {
        jdbc.update("CALL sp_iniciar_practica(?,?)", idPractica, idDocente);
    }

    /** sp_finalizar_practica: EN_CURSO -> FINALIZADO (cierra intentos, ausentes, califica). */
    @Override
    public void finalizarEnBd(Long idPractica, Long idDocente) {
        jdbc.update("CALL sp_finalizar_practica(?,?)", idPractica, idDocente);
    }

    /** sp_cerrar_practicas_vencidas: cierra automáticamente las EN_CURSO vencidas (RF-19). */
    public void cerrarVencidas() {
        jdbc.update("CALL sp_cerrar_practicas_vencidas()");
    }
}
