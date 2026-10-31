package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.AutorizacionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PreguntaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RespuestaOpcionRef;
import com.unihumboldt.nuclear.evaluacion.resultado.EvaluacionFacade;
import com.unihumboldt.nuclear.evaluacion.resultado.ResultadoRepository;

@Service
public class ParticipacionFacade {

    private final ParticipacionRepository repositorio;
    private final RespuestaSeleccionadaRepository respuestas;
    private final ResultadoRepository resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final EvaluacionFacade evaluacion;
    private final Clock clock;

    public ParticipacionFacade(
            ParticipacionRepository repositorio,
            RespuestaSeleccionadaRepository respuestas,
            ResultadoRepository resultados,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            EvaluacionFacade evaluacion,
            Clock clock) {
        this.repositorio = repositorio;
        this.respuestas = respuestas;
        this.resultados = resultados;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.evaluacion = evaluacion;
        this.clock = clock;
    }

    @Transactional
    public ParticipacionRespuesta iniciar(IniciarParticipacionRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esEstudiante()) {
            throw new AccesoDenegadoException("Solo estudiantes pueden iniciar participaciones.");
        }
        if (request == null || request.autorizacionId() == null) {
            throw new ReglaNegocioException("Se requiere autorizacionId.");
        }

        AutorizacionRef autorizacion = catalogo.buscarAutorizacion(request.autorizacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Autorización no encontrada."));
        if (!Objects.equals(autorizacion.estudianteId(), actor.estudianteId())) {
            throw new AccesoDenegadoException("La autorización no corresponde al estudiante autenticado.");
        }

        PracticaRef practica = catalogo.buscarPractica(autorizacion.practicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if ("CANCELADA".equals(practica.estado())) {
            throw new ReglaNegocioException("La práctica fue cancelada.");
        }
        if ("FINALIZADA".equals(practica.estado()) || ahora.isAfter(practica.fechaFin())) {
            throw new ReglaNegocioException("La práctica ya finalizó.");
        }

        List<Participacion> existentes = repositorio.findAllByAutorizacionIdOrderByNumeroIntentoAsc(autorizacion.id());
        Participacion enCurso = existentes.stream()
                .filter(p -> p.getEstado() == EstadoParticipacion.EN_CURSO)
                .findFirst()
                .orElse(null);
        if (enCurso != null) {
            return toRespuesta(enCurso, practica);
        }

        Participacion noIniciadaSinNota = existentes.stream()
                .filter(p -> p.getEstado() == EstadoParticipacion.NO_INICIADA)
                .filter(p -> !intentoConNota(p))
                .findFirst()
                .orElse(null);

        Rn17.validarVentanaInicio(practica, ahora);

        if (noIniciadaSinNota != null) {
            return toRespuesta(repositorio.save(MaquinaEstadoParticipacion.iniciar(noIniciadaSinNota, ahora)), practica);
        }

        if (existentes.size() >= Participacion.MAX_INTENTOS) {
            throw new ReglaNegocioException("Ya usaste los dos intentos permitidos.");
        }

        int siguiente = existentes.stream().mapToInt(Participacion::getNumeroIntento).max().orElse(0) + 1;
        Participacion nueva = new Participacion();
        nueva.setPracticaId(practica.id());
        nueva.setEstudianteId(autorizacion.estudianteId());
        nueva.setAutorizacionId(autorizacion.id());
        nueva.setNumeroIntento(siguiente);
        nueva.setEstado(EstadoParticipacion.NO_INICIADA);
        MaquinaEstadoParticipacion.iniciar(nueva, ahora);
        return toRespuesta(repositorio.save(nueva), practica);
    }

    @Transactional
    public void responder(Long participacionId, RegistrarRespuestaRequest request) {
        ActorActual actor = acceso.exigirActor();
        Participacion participacion = acceso.exigirDueñoEstudiante(participacionId, actor);
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO) {
            throw new ReglaNegocioException("No es posible alterar respuestas de un intento cerrado.");
        }
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));
        PreguntaRef pregunta = catalogo.preguntasDelCaso(practica.casoId()).stream()
                .filter(p -> p.id().equals(request.preguntaId()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La pregunta no pertenece al caso de esta práctica."));
        RespuestaOpcionRef opcion = catalogo.buscarRespuesta(request.respuestaId())
                .orElseThrow(() -> new ReglaNegocioException("La respuesta no existe."));
        if (!opcion.preguntaId().equals(pregunta.id())) {
            throw new ReglaNegocioException("La respuesta no pertenece a la pregunta indicada.");
        }
        RespuestaSeleccionada seleccion = respuestas
                .findByParticipacionIdAndPreguntaId(participacion.getId(), pregunta.id())
                .orElseGet(RespuestaSeleccionada::new);
        seleccion.setParticipacion(participacion);
        seleccion.setPreguntaId(pregunta.id());
        seleccion.setRespuestaId(opcion.id());
        respuestas.save(seleccion);
    }

    @Transactional
    public ParticipacionRespuesta finalizar(Long id) {
        ActorActual actor = acceso.exigirActor();
        Participacion participacion = acceso.exigirDueñoEstudiante(id, actor);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        MaquinaEstadoParticipacion.finalizar(participacion, ahora);
        participacion = repositorio.save(participacion);
        evaluacion.calcular(participacion);
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        return toRespuesta(participacion, practica);
    }

    @Transactional(readOnly = true)
    public ParticipacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Participacion participacion = acceso.exigirParticipacionVisible(id, actor);
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        return toRespuesta(participacion, practica);
    }

    @Transactional(readOnly = true)
    public List<ParticipacionRespuesta> listar(
            Long practicaId,
            Long estudianteId,
            EstadoParticipacion estado,
            Integer numeroIntento,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        ActorActual actor = acceso.exigirActor();
        return repositorio.findAllByOrderByIdDesc().stream()
                .filter(p -> acceso.visible(actor, p))
                .filter(p -> practicaId == null || Objects.equals(p.getPracticaId(), practicaId))
                .filter(p -> estudianteId == null || Objects.equals(p.getEstudianteId(), estudianteId))
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> numeroIntento == null || p.getNumeroIntento() == numeroIntento)
                .filter(p -> fechaDesde == null || (p.getInicio() != null && !p.getInicio().toLocalDate().isBefore(fechaDesde)))
                .filter(p -> fechaHasta == null || (p.getInicio() != null && !p.getInicio().toLocalDate().isAfter(fechaHasta)))
                .map(p -> toRespuesta(p, catalogo.buscarPractica(p.getPracticaId()).orElse(null)))
                .toList();
    }

    private boolean intentoConNota(Participacion participacion) {
        return participacion.getId() != null
                && resultados.findByParticipacionId(participacion.getId()).isPresent();
    }

    private ParticipacionRespuesta toRespuesta(Participacion participacion, PracticaRef practica) {
        EstudianteRef estudiante = catalogo.buscarEstudiante(participacion.getEstudianteId()).orElse(null);
        return new ParticipacionRespuesta(
                participacion.getId(),
                participacion.getPracticaId(),
                practica != null ? practica.nombre() : null,
                participacion.getEstudianteId(),
                estudiante != null ? estudiante.correo() : null,
                estudiante != null ? estudiante.nombreCompleto() : null,
                participacion.getAutorizacionId(),
                participacion.getNumeroIntento(),
                participacion.getInicio(),
                participacion.getFin(),
                participacion.getTiempoUsadoSeg(),
                participacion.getEstado());
    }
}
