package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;

@Service
public class RetroalimentacionFacade {

    private final RetroalimentacionRepository repositorio;
    private final CatalogoAcademico catalogo;
    private final PublicadorEventos publicador;
    private final Clock clock;

    public RetroalimentacionFacade(
            RetroalimentacionRepository repositorio,
            CatalogoAcademico catalogo,
            PublicadorEventos publicador,
            Clock clock) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public RetroalimentacionRespuesta guardar(
            ActorActual actor,
            Participacion participacion,
            RegistrarRetroalimentacionRequest request) {
        if (request.texto() == null || request.texto().isBlank()) {
            throw new ReglaNegocioException("El texto de retroalimentación es obligatorio.");
        }
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));
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
        return describir(retro);
    }

    @Transactional(readOnly = true)
    public Retroalimentacion exigir(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Retroalimentación no encontrada."));
    }

    @Transactional(readOnly = true)
    public RetroalimentacionRespuesta describir(Retroalimentacion retro) {
        return toRespuesta(retro);
    }

    @Transactional(readOnly = true)
    public List<RetroalimentacionRespuesta> listar(Long practicaId, Long participacionId) {
        return repositorio.findAllByOrderByIdDesc().stream()
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
