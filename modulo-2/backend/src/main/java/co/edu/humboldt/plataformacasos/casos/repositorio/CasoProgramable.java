package co.edu.humboldt.plataformacasos.casos.repositorio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;

import java.time.LocalDateTime;

/**
 * Lo que el Módulo 3 necesita para programar un caso (RF-04, RF-18): el tiempo estimado limita el
 * tiempo máximo de la sesión. No contiene preguntas, respuestas ni justificaciones.
 */
public record CasoProgramable(Long id, String nombre, Integer tiempoEstimadoMinutos, Long docenteId,
                              EstadoCaso estado, boolean programable, LocalDateTime fechaPublicacion) {

    /** Usado por la consulta JPQL. */
    public CasoProgramable(Long id, String nombre, Integer tiempoEstimadoMinutos, Long docenteId, EstadoCaso estado,
                           LocalDateTime fechaPublicacion) {
        this(id, nombre, tiempoEstimadoMinutos, docenteId, estado, estado.disponibleParaProgramar(), fechaPublicacion);
    }

    public static CasoProgramable de(CasoEstudio caso) {
        return new CasoProgramable(caso.getId(), caso.getNombre(), caso.getTiempoEstimadoMinutos(),
                caso.getDocenteId(), caso.getEstado(), caso.getFechaPublicacion());
    }
}
