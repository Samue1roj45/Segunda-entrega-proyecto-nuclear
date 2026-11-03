package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;

/**
 * Protection Proxy RF-26: solo el docente de la práctica registra;
 * la consulta queda limitada al dueño o a ese docente.
 */
@Service
public class RetroalimentacionProxy implements GestionRetroalimentacion {

    private final RetroalimentacionFacade real;
    private final AccesoEvaluacion acceso;

    public RetroalimentacionProxy(RetroalimentacionFacade real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public RetroalimentacionRespuesta registrar(RegistrarRetroalimentacionRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esDocente() && !actor.esAdmin()) {
            throw new AccesoDenegadoException("Solo el docente puede registrar retroalimentación.");
        }
        Participacion participacion = acceso.exigirParticipacion(request.participacionId());
        acceso.exigirPracticaDocente(participacion.getPracticaId(), actor);
        return real.guardar(actor, participacion, request);
    }

    @Override
    public RetroalimentacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Retroalimentacion retro = real.exigir(id);
        if (!acceso.visible(actor, retro.getParticipacion())) {
            throw new RecursoNoEncontradoException("Retroalimentación no encontrada.");
        }
        return real.describir(retro);
    }

    @Override
    public List<RetroalimentacionRespuesta> listar(Long practicaId, Long participacionId) {
        ActorActual actor = acceso.exigirActor();
        return real.listar(practicaId, participacionId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }
}
