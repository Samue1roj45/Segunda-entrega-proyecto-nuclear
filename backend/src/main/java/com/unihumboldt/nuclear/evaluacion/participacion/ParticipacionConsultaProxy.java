package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;

/**
 * Protection Proxy RF-15: el estudiante solo ve sus intentos;
 * el docente solo los de sus prácticas.
 */
@Service
public class ParticipacionConsultaProxy implements ConsultaParticipacion {

    private final ParticipacionFacade real;
    private final AccesoEvaluacion acceso;

    public ParticipacionConsultaProxy(ParticipacionFacade real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public ParticipacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Participacion participacion = acceso.exigirParticipacion(id);
        acceso.exigirVisible(participacion, actor);
        return real.describir(participacion);
    }

    @Override
    public List<ParticipacionRespuesta> listar(
            Long practicaId,
            Long estudianteId,
            EstadoParticipacion estado,
            Integer numeroIntento,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        ActorActual actor = acceso.exigirActor();
        return real.listar(practicaId, estudianteId, estado, numeroIntento, fechaDesde, fechaHasta).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }
}
