package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;

@Service
public class RetroalimentacionFacade {

    private final RetroalimentacionRepository repositorio;
    private final AccesoEvaluacion acceso;
    private final PublicadorEventos publicador;
    private final Clock clock;

    public RetroalimentacionFacade(
            RetroalimentacionRepository repositorio,
            AccesoEvaluacion acceso,
            PublicadorEventos publicador,
            Clock clock) {
        this.repositorio = repositorio;
        this.acceso = acceso;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public RetroalimentacionRespuesta registrar(RegistrarRetroalimentacionRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esDocente() && !actor.esAdmin()) {
            throw new AccesoDenegadoException("Solo el docente puede registrar retroalimentación.");
        }
        Participacion participacion = acceso.exigirParticipacion(request.participacionId());
        PracticaRef practica = acceso.exigirPracticaDocente(participacion.getPracticaId(), actor);
        if (request.texto() == null || request.texto().isBlank()) {
            throw new ReglaNegocioException("El texto de retroalimentación es obligatorio.");
        }
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        Retroalimentacion retro = repositorio.findByParticipacionId(participacion.getId()).orElseGet(Retroalimentacion::new);
        retro.setParticipacion(participacion);
        retro.setEstudianteId(participacion.getEstudianteId());
        retro.setPracticaId(participacion.getPracticaId());
        retro.setDocenteId(actor.docenteId() == null ? practica.docenteId() : actor.docenteId());
        retro.setTexto(request.texto().trim());
        if (retro.getFechaCreacion() == null) {
            retro.setFechaCreacion(ahora);
        }
        retro.setFechaActualizacion(ahora);
        retro = repositorio.save(retro);

        publicador.publicar(new EventoAcademico(
                TipoNotificacion.RETROALIMENTACION,
                "RETROALIMENTACION:" + participacion.getId(),
                participacion.getPracticaId(),
                participacion.getEstudianteId(),
                participacion.getId(),
                "Retroalimentacion de " + practica.nombre(),
                "El docente publico retroalimentacion del intento " + participacion.getNumeroIntento()
                        + " de la practica '" + practica.nombre() + "'."));
        return toRespuesta(retro);
    }

    @Transactional(readOnly = true)
    public RetroalimentacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Retroalimentacion retro = repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Retroalimentación no encontrada."));
        if (!acceso.visible(actor, retro.getParticipacion())) {
            throw new RecursoNoEncontradoException("Retroalimentación no encontrada.");
        }
        return toRespuesta(retro);
    }

    @Transactional(readOnly = true)
    public List<RetroalimentacionRespuesta> listar(Long practicaId, Long participacionId) {
        ActorActual actor = acceso.exigirActor();
        return repositorio.findAllByOrderByIdDesc().stream()
                .filter(r -> acceso.visible(actor, r.getParticipacion()))
                .filter(r -> practicaId == null || Objects.equals(r.getPracticaId(), practicaId))
                .filter(r -> participacionId == null || Objects.equals(r.getParticipacion().getId(), participacionId))
                .map(this::toRespuesta)
                .toList();
    }

    private RetroalimentacionRespuesta toRespuesta(Retroalimentacion retro) {
        Participacion p = retro.getParticipacion();
        return new RetroalimentacionRespuesta(
                retro.getId(),
                p.getId(),
                p.getNumeroIntento(),
                retro.getEstudianteId(),
                retro.getPracticaId(),
                retro.getDocenteId(),
                retro.getTexto(),
                retro.getFechaCreacion(),
                retro.getFechaActualizacion());
    }
}
