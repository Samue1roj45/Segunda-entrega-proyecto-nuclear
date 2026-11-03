package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Sujeto del Proxy de consulta de participaciones (RF-15).
 */
public interface ConsultaParticipacion {

    ParticipacionRespuesta obtener(Long id);

    List<ParticipacionRespuesta> listar(
            Long practicaId,
            Long estudianteId,
            EstadoParticipacion estado,
            Integer numeroIntento,
            LocalDate fechaDesde,
            LocalDate fechaHasta);
}
