package com.example.demo.repository;

import com.example.demo.dto.ReporteParticipacionDto;
import com.example.demo.model.fila.CalificacionPreguntaFila;
import com.example.demo.model.fila.ResultadoFila;
import com.example.demo.model.fila.Mapeo;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: acceso a resultados, calificación por pregunta, retroalimentación del docente
 y reporte de participación (RF-12/13/26). La BD ya califica al finalizar un intento;
 aquí solo se lee y se registra la retroalimentación.
 */
@Repository
public class ResultadoRepository {

    private final JdbcTemplate jdbc;

    public ResultadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ResultadoFila> porParticipacion(Long idParticipacion) {
        return jdbc.query("SELECT * FROM resultados WHERE id_participacion = ?",
                ResultadoFila.MAPA, idParticipacion).stream().findFirst();
    }

    public List<CalificacionPreguntaFila> calificacionesPorPregunta(Long idResultado) {
        return jdbc.query("SELECT * FROM calificaciones_pregunta WHERE id_resultado = ? ORDER BY id_pregunta",
                CalificacionPreguntaFila.MAPA, idResultado);
    }

    /** sp_calificar_participacion: recalcula la nota del intento. */
    public void calificar(Long idParticipacion) {
        jdbc.update("CALL sp_calificar_participacion(?)", idParticipacion);
    }

    /** sp_registrar_feedback_docente: exige permiso REGISTRAR_RETROALIMENTACION y que el intento haya terminado. */
    public void registrarFeedback(Long idParticipacion, Long idDocente, String texto) {
        jdbc.update("CALL sp_registrar_feedback_docente(?,?,?)", idParticipacion, idDocente, texto);
    }

    /** sp_reporte_participacion: exige permiso DESCARGAR_REPORTES y ser docente de la sesión o creador del caso. */
    public List<ReporteParticipacionDto> reporte(Long idPractica, Long idDocente) {
        return jdbc.query("CALL sp_reporte_participacion(?,?)",
                (rs, n) -> new ReporteParticipacionDto(
                        Mapeo.largo(rs, "id_practica"), Mapeo.largo(rs, "id_participacion"),
                        Mapeo.largo(rs, "id_estudiante"), Mapeo.texto(rs, "estudiante"),
                        Mapeo.texto(rs, "correo_estudiante"), Mapeo.largo(rs, "numero_intento"),
                        Mapeo.texto(rs, "estado_participacion"), Mapeo.fecha(rs, "hora_inicio"),
                        Mapeo.fecha(rs, "hora_fin"), Mapeo.decimal(rs, "nota"),
                        Mapeo.texto(rs, "feedback_docente"), Mapeo.fecha(rs, "fecha_calculo"),
                        Mapeo.entero(rs, "preguntas_respondidas"), Mapeo.entero(rs, "total_preguntas"),
                        Mapeo.decimal(rs, "porcentaje_progreso")),
                idPractica, idDocente);
    }
}
