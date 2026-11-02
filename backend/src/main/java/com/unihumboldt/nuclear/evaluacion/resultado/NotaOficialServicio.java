package com.unihumboldt.nuclear.evaluacion.resultado;

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
import com.unihumboldt.nuclear.evaluacion.participacion.ParticipacionRepository;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;

/**
 * RF-14: el docente elige qué intento es la nota oficial. No reescribe notaFinal.
 */
@Service
public class NotaOficialServicio {

    private final NotaOficialRepository notasOficiales;
    private final ResultadoRepository resultados;
    private final ParticipacionRepository participaciones;
    private final AccesoEvaluacion acceso;
    private final Clock clock;

    public NotaOficialServicio(
            NotaOficialRepository notasOficiales,
            ResultadoRepository resultados,
            ParticipacionRepository participaciones,
            AccesoEvaluacion acceso,
            Clock clock) {
        this.notasOficiales = notasOficiales;
        this.resultados = resultados;
        this.participaciones = participaciones;
        this.acceso = acceso;
        this.clock = clock;
    }

    @Transactional
    public NotaOficialRespuesta seleccionar(SeleccionarNotaOficialRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esDocente() && !actor.esAdmin()) {
            throw new AccesoDenegadoException("Solo el docente puede seleccionar la nota oficial.");
        }
        PracticaRef practica = acceso.exigirPracticaDocente(request.practicaId(), actor);
        Participacion participacion = participaciones.findById(request.participacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada."));
        if (!Objects.equals(participacion.getPracticaId(), practica.id())) {
            throw new ReglaNegocioException("El intento no pertenece a esta práctica.");
        }
        if (!Objects.equals(participacion.getEstudianteId(), request.estudianteId())) {
            throw new ReglaNegocioException("El intento no pertenece a este estudiante.");
        }
        Resultado resultado = resultados.findByParticipacionId(participacion.getId())
                .orElseThrow(() -> new ReglaNegocioException("El intento no tiene resultado para seleccionar."));
        NotaOficial seleccion = notasOficiales
                .findByPracticaIdAndEstudianteId(practica.id(), request.estudianteId())
                .orElseGet(NotaOficial::new);
        seleccion.setPracticaId(practica.id());
        seleccion.setEstudianteId(request.estudianteId());
        seleccion.setParticipacion(participacion);
        seleccion.setDocenteId(actor.docenteId() == null ? practica.docenteId() : actor.docenteId());
        seleccion.setFechaSeleccion(OffsetDateTime.now(clock));
        return toOficial(notasOficiales.save(seleccion), resultado);
    }

    @Transactional(readOnly = true)
    public List<NotaOficialRespuesta> listar(Long practicaId, Long estudianteId) {
        return notasOficiales.findAllByOrderByIdDesc().stream()
                .filter(n -> practicaId == null || Objects.equals(n.getPracticaId(), practicaId))
                .filter(n -> estudianteId == null || Objects.equals(n.getEstudianteId(), estudianteId))
                .map(n -> toOficial(n, resultados.findByParticipacionId(n.getParticipacion().getId()).orElse(null)))
                .toList();
    }

    private NotaOficialRespuesta toOficial(NotaOficial seleccion, Resultado resultado) {
        Participacion participacion = seleccion.getParticipacion();
        return new NotaOficialRespuesta(
                seleccion.getId(),
                seleccion.getPracticaId(),
                seleccion.getEstudianteId(),
                participacion.getId(),
                participacion.getNumeroIntento(),
                resultado == null ? null : resultado.getNotaFinal(),
                seleccion.getDocenteId(),
                seleccion.getFechaSeleccion());
    }
}
