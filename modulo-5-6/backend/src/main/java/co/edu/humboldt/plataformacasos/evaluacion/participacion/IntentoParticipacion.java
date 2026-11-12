package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.time.OffsetDateTime;

/**
 * Vista de solo lectura de un intento (participación) que este módulo comparte con el módulo 4.
 * No expone la entidad JPA: el módulo 4 no puede cambiar el estado por fuera de la máquina de estados.
 */
public record IntentoParticipacion(Long id, Long practicaId, Long autorizacionId, Long estudianteId,
                                   int numeroIntento, EstadoParticipacion estado,
                                   OffsetDateTime inicio, OffsetDateTime fin) {

    static IntentoParticipacion de(Participacion p) {
        return new IntentoParticipacion(p.getId(), p.getPracticaId(), p.getAutorizacionId(), p.getEstudianteId(),
                p.getNumeroIntento(), p.getEstado(), p.getInicio(), p.getFin());
    }

    public boolean enCurso() {
        return estado == EstadoParticipacion.EN_CURSO;
    }

    /** Una ausencia (NO_INICIADA) no cuenta como intento usado (RF-24, RF-25). */
    public boolean cuentaComoIntento() {
        return estado != EstadoParticipacion.NO_INICIADA;
    }
}
