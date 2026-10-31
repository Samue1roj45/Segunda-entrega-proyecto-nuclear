package com.unihumboldt.nuclear.evaluacion.participacion;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.ActorActualResolver;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;

@Component
public class AccesoEvaluacion {

    private final ActorActualResolver actores;
    private final CatalogoAcademico catalogo;
    private final ParticipacionRepository participaciones;

    public AccesoEvaluacion(
            ActorActualResolver actores,
            CatalogoAcademico catalogo,
            ParticipacionRepository participaciones) {
        this.actores = actores;
        this.catalogo = catalogo;
        this.participaciones = participaciones;
    }

    public ActorActual exigirActor() {
        return actores.actual().orElseThrow(() -> new AccesoDenegadoException("No se pudo resolver el actor autenticado."));
    }

    public boolean visible(ActorActual actor, Participacion participacion) {
        if (actor.esAdmin()) {
            return true;
        }
        if (actor.esEstudiante()) {
            return Objects.equals(participacion.getEstudianteId(), actor.estudianteId());
        }
        if (actor.esDocente()) {
            return catalogo.buscarPractica(participacion.getPracticaId())
                    .map(practica -> Objects.equals(practica.docenteId(), actor.docenteId()))
                    .orElse(false);
        }
        return false;
    }

    public Participacion exigirParticipacion(Long id) {
        return participaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada."));
    }

    public Participacion exigirParticipacionVisible(Long id, ActorActual actor) {
        Participacion participacion = exigirParticipacion(id);
        if (!visible(actor, participacion)) {
            throw new RecursoNoEncontradoException("Participación no encontrada.");
        }
        return participacion;
    }

    /**
     * Mutaciones de intento (responder, finalizar): solo el estudiante dueño.
     */
    public Participacion exigirDueñoEstudiante(Long id, ActorActual actor) {
        Participacion participacion = exigirParticipacionVisible(id, actor);
        if (!actor.esEstudiante() || !Objects.equals(participacion.getEstudianteId(), actor.estudianteId())) {
            throw new AccesoDenegadoException("Solo el estudiante dueño puede responder o finalizar este intento.");
        }
        return participacion;
    }

    public PracticaRef exigirPracticaDocente(Long practicaId, ActorActual actor) {
        PracticaRef practica = catalogo.buscarPractica(practicaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));
        if (actor.esAdmin()) {
            return practica;
        }
        if (actor.esDocente() && Objects.equals(practica.docenteId(), actor.docenteId())) {
            return practica;
        }
        throw new AccesoDenegadoException("No autorizado para esta práctica.");
    }
}
