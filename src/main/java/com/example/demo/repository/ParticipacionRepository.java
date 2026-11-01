package com.example.demo.repository;

import com.example.demo.dto.ContextoDto;
import com.example.demo.dto.FinalizacionDto;
import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.TiempoRestanteDto;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.DatosTiempoSesion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Para qué sirve: acceso a la BD para la participación del estudiante: iniciar, contexto,
 respuestas, progreso, tiempo, finalización y expiración. Las reglas (sesión en curso,
 tiempo máximo, ingreso tardío, intentos) las aplican los triggers de la BD.
*/
@Repository
public class ParticipacionRepository {

    private static final RowMapper<ProgresoDto> MAPA_PROGRESO = (rs, n) -> new ProgresoDto(
            rs.getLong("id_participacion"), rs.getInt("total_preguntas"),
            rs.getInt("preguntas_respondidas"), rs.getBigDecimal("porcentaje_progreso"));

    private final JdbcTemplate jdbc;

    public ParticipacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- Iniciar / responder / progreso (RF-10, RF-11) ----------

    /** sp_iniciar_participacion: crea el intento (o devuelve el que sigue EN_CURSO). */
    public Long iniciar(Long idAutorizacion) {
        return jdbc.queryForObject("CALL sp_iniciar_participacion(?)", Long.class, idAutorizacion);
    }

    /** sp_responder: guarda o cambia la respuesta (una fila por pregunta) y devuelve el progreso. */
    public ProgresoDto responder(Long idParticipacion, Long idPregunta, Long idRespuesta) {
        return jdbc.queryForObject("CALL sp_responder(?,?,?)", MAPA_PROGRESO,
                idParticipacion, idPregunta, idRespuesta);
    }

    /** Lee vw_progreso_participacion. */
    public ProgresoDto consultarProgreso(Long idParticipacion) {
        List<ProgresoDto> r = jdbc.query("""
                SELECT id_participacion, total_preguntas, preguntas_respondidas, porcentaje_progreso
                  FROM vw_progreso_participacion WHERE id_participacion = ?
                """, MAPA_PROGRESO, idParticipacion);
        if (r.isEmpty()) {
            throw new ReglaNegocioException("La participación no existe");
        }
        return r.get(0);
    }

    // ---------- RF-22: ingreso tardío ----------

    public boolean tieneParticipacionEnCurso(Long idAutorizacion) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM participaciones
                 WHERE id_autorizacion = ? AND estado_participacion = 'EN_CURSO'
                """, Integer.class, idAutorizacion);
        return n != null && n > 0;
    }

    public Optional<DatosTiempoSesion> datosTiempo(Long idAutorizacion) {
        return jdbc.query("""
                SELECT pr.fecha_inicio, pr.tiempo_maximo_minutos, pr.porcentaje_limite_ingreso_tardio,
                       pr.estado_practica, a.estado AS estado_aut, NOW() AS ahora
                  FROM autorizaciones a
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                 WHERE a.id_autorizacion = ?
                """,
                (rs, n) -> new DatosTiempoSesion(
                        rs.getObject("fecha_inicio", LocalDateTime.class),
                        rs.getInt("tiempo_maximo_minutos"),
                        rs.getBigDecimal("porcentaje_limite_ingreso_tardio"),
                        rs.getString("estado_practica"), rs.getString("estado_aut"),
                        rs.getObject("ahora", LocalDateTime.class)),
                idAutorizacion).stream().findFirst();
    }

    // ---------- RF-23: tiempo máximo ----------

    /** Tiempo restante del intento (la hora actual la da la BD, no Java). */
    public Optional<TiempoRestanteDto> consultarTiempo(Long idParticipacion) {
        return jdbc.query("""
                SELECT pa.hora_inicio, pr.tiempo_maximo_minutos, pa.estado_participacion, NOW() AS ahora
                  FROM participaciones pa
                  JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                 WHERE pa.id_participacion = ?
                """,
                (rs, n) -> {
                    LocalDateTime inicio = rs.getObject("hora_inicio", LocalDateTime.class);
                    LocalDateTime ahora = rs.getObject("ahora", LocalDateTime.class);
                    String estado = rs.getString("estado_participacion");
                    boolean enCurso = "EN_CURSO".equals(estado);
                    LocalDateTime limite = inicio == null ? null
                            : inicio.plusMinutes(rs.getInt("tiempo_maximo_minutos"));
                    long segundos = (limite == null || !enCurso) ? 0
                            : Math.max(0, Duration.between(ahora, limite).getSeconds());
                    boolean expirada = !enCurso || (limite != null && ahora.isAfter(limite));
                    return new TiempoRestanteDto(idParticipacion, estado, inicio, limite, segundos, expirada);
                },
                idParticipacion).stream().findFirst();
    }

    /** sp_finalizar_participacion(id, 0): el estudiante termina su intento; se califica. */
    public FinalizacionDto finalizar(Long idParticipacion) {
        jdbc.update("CALL sp_finalizar_participacion(?, 0)", idParticipacion);
        return jdbc.queryForObject("""
                SELECT id_participacion, estado_participacion, hora_fin
                  FROM participaciones WHERE id_participacion = ?
                """,
                (rs, n) -> new FinalizacionDto(rs.getLong(1), rs.getString(2),
                        rs.getObject(3, LocalDateTime.class)),
                idParticipacion);
    }

    /** sp_expirar_participaciones: cierra los intentos EN_CURSO cuyo tiempo máximo se agotó. */
    public void expirarVencidas() {
        jdbc.update("CALL sp_expirar_participaciones()");
    }

    // ---------- Contexto del caso (RF-10) ----------

    private record Cabecera(String nombre, String contexto, String desarrollo) { }

    private record Fila(Long idEscenario, int ordenEscenario, String nombreEscenario,
                        String contextoEscenario, Long idPregunta, int ordenPregunta,
                        String enunciado, Long idRespuesta, int ordenRespuesta,
                        String textoRespuesta, Long seleccionada) { }

    public ContextoDto consultarContexto(Long idParticipacion) {
        List<Cabecera> cab = jdbc.query("""
                SELECT c.nombre_caso, c.contexto_historia, c.desarrollo_situacional
                  FROM participaciones pa
                  JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                  JOIN casos_estudio c ON c.id_caso = pr.id_caso
                 WHERE pa.id_participacion = ?
                """,
                (rs, n) -> new Cabecera(rs.getString(1), rs.getString(2), rs.getString(3)),
                idParticipacion);
        if (cab.isEmpty()) {
            throw new ReglaNegocioException("La participación no existe");
        }

        // NO se selecciona es_correcta ni retroalimentacion: no deben llegar al estudiante.
        List<Fila> filas = jdbc.query("""
                SELECT e.id_escenario, e.orden_escenario, e.nombre_escenario, e.contexto_escenario,
                       q.id_pregunta, q.orden_pregunta, q.enunciado_pregunta,
                       r.id_respuesta, r.orden_respuesta, r.texto_respuesta,
                       rr.id_respuesta AS seleccionada
                  FROM participaciones pa
                  JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
                  JOIN practicas pr ON pr.id_practica = a.id_practica
                  JOIN escenarios e ON e.id_caso = pr.id_caso
                  JOIN preguntas q ON q.id_escenario = e.id_escenario
                  JOIN respuestas r ON r.id_pregunta = q.id_pregunta
                  LEFT JOIN registros_respuesta rr
                         ON rr.id_participacion = pa.id_participacion AND rr.id_pregunta = q.id_pregunta
                 WHERE pa.id_participacion = ?
                 ORDER BY e.orden_escenario, q.orden_pregunta, r.orden_respuesta
                """, this::mapearFila, idParticipacion);

        List<ContextoDto.Escenario> escenarios = filas.stream()
                .collect(Collectors.groupingBy(Fila::idEscenario, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(this::armarEscenario)
                .toList();

        Cabecera c = cab.get(0);
        return new ContextoDto(c.nombre(), c.contexto(), c.desarrollo(), escenarios);
    }

    private Fila mapearFila(ResultSet rs, int n) throws SQLException {
        long sel = rs.getLong("seleccionada");
        Long seleccionada = rs.wasNull() ? null : sel;
        return new Fila(rs.getLong("id_escenario"), rs.getInt("orden_escenario"),
                rs.getString("nombre_escenario"), rs.getString("contexto_escenario"),
                rs.getLong("id_pregunta"), rs.getInt("orden_pregunta"),
                rs.getString("enunciado_pregunta"), rs.getLong("id_respuesta"),
                rs.getInt("orden_respuesta"), rs.getString("texto_respuesta"), seleccionada);
    }

    private ContextoDto.Escenario armarEscenario(List<Fila> deEscenario) {
        Fila e = deEscenario.get(0);
        List<ContextoDto.Pregunta> preguntas = deEscenario.stream()
                .collect(Collectors.groupingBy(Fila::idPregunta, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(dePregunta -> {
                    Fila p = dePregunta.get(0);
                    List<ContextoDto.Opcion> opciones = dePregunta.stream()
                            .map(o -> new ContextoDto.Opcion(o.idRespuesta(), o.ordenRespuesta(), o.textoRespuesta()))
                            .toList();
                    return new ContextoDto.Pregunta(p.idPregunta(), p.ordenPregunta(),
                            p.enunciado(), p.seleccionada(), opciones);
                })
                .toList();
        return new ContextoDto.Escenario(e.idEscenario(), e.ordenEscenario(),
                e.nombreEscenario(), e.contextoEscenario(), preguntas);
    }

    // ---------- Verificaciones de pertenencia (las usa el Proxy) ----------

    public boolean participacionDeEstudiante(Long idParticipacion, Long idEstudiante) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM participaciones pa
                  JOIN autorizaciones a ON a.id_autorizacion = pa.id_autorizacion
                 WHERE pa.id_participacion = ? AND a.id_estudiante = ?
                """, Integer.class, idParticipacion, idEstudiante);
        return n != null && n > 0;
    }

    public boolean autorizacionDeEstudiante(Long idAutorizacion, Long idEstudiante) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM autorizaciones WHERE id_autorizacion = ? AND id_estudiante = ?",
                Integer.class, idAutorizacion, idEstudiante);
        return n != null && n > 0;
    }
}
