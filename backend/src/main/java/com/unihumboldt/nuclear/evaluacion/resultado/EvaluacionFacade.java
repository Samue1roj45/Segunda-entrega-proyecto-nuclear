package com.unihumboldt.nuclear.evaluacion.resultado;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.AutorizacionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PreguntaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RespuestaOpcionRef;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.MaquinaEstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ParticipacionRepository;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;
import com.unihumboldt.nuclear.evaluacion.notificacion.EventoAcademico;
import com.unihumboldt.nuclear.evaluacion.notificacion.PublicadorEventos;
import com.unihumboldt.nuclear.evaluacion.notificacion.ResumenEnvio;
import com.unihumboldt.nuclear.evaluacion.notificacion.TipoNotificacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionada;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

@Service
public class EvaluacionFacade {

    private final ResultadoRepository resultados;
    private final ParticipacionRepository participaciones;
    private final RespuestaSeleccionadaRepository respuestas;
    private final NotaOficialRepository notasOficiales;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final PublicadorEventos publicador;
    private final Clock clock;

    public EvaluacionFacade(
            ResultadoRepository resultados,
            ParticipacionRepository participaciones,
            RespuestaSeleccionadaRepository respuestas,
            NotaOficialRepository notasOficiales,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            PublicadorEventos publicador,
            Clock clock) {
        this.resultados = resultados;
        this.participaciones = participaciones;
        this.respuestas = respuestas;
        this.notasOficiales = notasOficiales;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.publicador = publicador;
        this.clock = clock;
    }

    @Transactional
    public Resultado calcular(Participacion participacion) {
        return resultados.findByParticipacionId(participacion.getId()).orElseGet(() -> persistirNota(participacion));
    }

    private Resultado persistirNota(Participacion participacion) {
        EstrategiaEvaluacion estrategia = estrategiaPara(participacion);
        MetricasEvaluacion metricas = estrategia.calcular(participacion);
        Resultado resultado = new Resultado();
        resultado.setParticipacion(participacion);
        resultado.setCorrectas(metricas.correctas());
        resultado.setIncorrectas(metricas.incorrectas());
        resultado.setNoRespondidas(metricas.noRespondidas());
        resultado.setPesoObtenido(metricas.pesoObtenido());
        resultado.setPesoTotal(metricas.pesoTotal());
        resultado.setEscalaMaxima(metricas.escalaMaxima());
        resultado.setNotaFinal(metricas.notaFinal());
        resultado.setFechaCalculo(OffsetDateTime.now(clock));
        return resultados.save(resultado);
    }

    @Transactional(readOnly = true)
    public Resultado exigirResultado(Long id) {
        return resultados.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Resultado no encontrado."));
    }

    @Transactional(readOnly = true)
    public ResultadoRespuesta consultarCerrado(Resultado resultado) {
        if (resultado.getParticipacion().getEstado() == EstadoParticipacion.EN_CURSO) {
            throw new ReglaNegocioException("El resultado aún no está disponible para consulta.");
        }
        return toRespuesta(resultado, mapaOficiales());
    }

    @Transactional(readOnly = true)
    public List<ResultadoRespuesta> listar(Long practicaId, Long estudianteId) {
        Map<String, NotaOficial> oficiales = mapaOficiales();
        return resultados.findAllByOrderByIdDesc().stream()
                .filter(r -> practicaId == null || Objects.equals(r.getParticipacion().getPracticaId(), practicaId))
                .filter(r -> estudianteId == null || Objects.equals(r.getParticipacion().getEstudianteId(), estudianteId))
                .map(r -> toRespuesta(r, oficiales))
                .toList();
    }

    @Transactional
    public NotaOficialRespuesta seleccionarOficial(SeleccionarNotaOficialRequest request) {
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
        seleccion = notasOficiales.save(seleccion);
        return toOficial(seleccion, resultado);
    }

    @Transactional(readOnly = true)
    public List<NotaOficialRespuesta> listarOficiales(Long practicaId, Long estudianteId) {
        return notasOficiales.findAllByOrderByIdDesc().stream()
                .filter(n -> practicaId == null || Objects.equals(n.getPracticaId(), practicaId))
                .filter(n -> estudianteId == null || Objects.equals(n.getEstudianteId(), estudianteId))
                .map(n -> toOficial(n, resultados.findByParticipacionId(n.getParticipacion().getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public EvaluacionPendientesRespuesta evaluarPendientes(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        int incompletas = 0;
        int ausencias = 0;
        int omitidas = 0;
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            List<Participacion> existentes = participaciones.findAllByAutorizacionIdOrderByNumeroIntentoAsc(autorizacion.id());
            List<Participacion> enCurso = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.EN_CURSO)
                    .toList();
            if (!enCurso.isEmpty()) {
                for (Participacion part : enCurso) {
                    MaquinaEstadoParticipacion.marcarIncompleta(part, OffsetDateTime.now(clock));
                    participaciones.save(part);
                    calcular(part);
                    incompletas++;
                }
                continue;
            }
            if (existentes.stream().anyMatch(Participacion::esTerminal)) {
                omitidas++;
                continue;
            }
            Participacion noIniciada = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.NO_INICIADA)
                    .findFirst()
                    .orElse(null);
            if (noIniciada == null) {
                noIniciada = new Participacion();
                noIniciada.setPracticaId(practica.id());
                noIniciada.setEstudianteId(autorizacion.estudianteId());
                noIniciada.setAutorizacionId(autorizacion.id());
                noIniciada.setNumeroIntento(1);
                noIniciada.setEstado(EstadoParticipacion.NO_INICIADA);
                noIniciada = participaciones.save(noIniciada);
            }
            if (resultados.findByParticipacionId(noIniciada.getId()).isPresent()) {
                omitidas++;
                continue;
            }
            calcular(noIniciada);
            ausencias++;
        }
        return new EvaluacionPendientesRespuesta(incompletas, ausencias, omitidas);
    }

    @Transactional
    public ResumenEnvio publicarCalificaciones(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        for (Resultado resultado : resultados.findAllByOrderByIdDesc()) {
            Participacion participacion = resultado.getParticipacion();
            if (!Objects.equals(participacion.getPracticaId(), practica.id())) {
                continue;
            }
            String cuerpo = "Se publico la calificacion del intento " + participacion.getNumeroIntento()
                    + " de '" + practica.nombre() + "'. Nota: " + resultado.getNotaFinal()
                    + " / " + resultado.getEscalaMaxima() + ".";
            resumen = resumen.acumular(publicador.publicar(new EventoAcademico(
                    TipoNotificacion.CALIFICACION,
                    "CALIFICACION:" + participacion.getId(),
                    practica.id(),
                    participacion.getEstudianteId(),
                    participacion.getId(),
                    "Calificacion publicada: " + practica.nombre(),
                    cuerpo)));
        }
        return resumen;
    }

    private EstrategiaEvaluacion estrategiaPara(Participacion participacion) {
        if (participacion.getEstado() == EstadoParticipacion.FINALIZADA) {
            return new EvaluacionCompletaStrategy(catalogo, respuestas);
        }
        if (participacion.getEstado() == EstadoParticipacion.INCOMPLETA) {
            return new EvaluacionIncompletaStrategy(catalogo, respuestas);
        }
        return new EvaluacionAusenciaStrategy(catalogo);
    }

    private Map<String, NotaOficial> mapaOficiales() {
        return notasOficiales.findAllByOrderByIdDesc().stream()
                .collect(Collectors.toMap(
                        n -> clave(n.getPracticaId(), n.getEstudianteId()),
                        n -> n,
                        (a, b) -> a));
    }

    private static String clave(Long practicaId, Long estudianteId) {
        return practicaId + ":" + estudianteId;
    }

    private ResultadoRespuesta toRespuesta(Resultado resultado, Map<String, NotaOficial> oficiales) {
        Participacion participacion = resultado.getParticipacion();
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        EstudianteRef estudiante = catalogo.buscarEstudiante(participacion.getEstudianteId()).orElse(null);
        NotaOficial seleccion = oficiales.get(clave(participacion.getPracticaId(), participacion.getEstudianteId()));
        boolean oficial = seleccion != null
                && Objects.equals(seleccion.getParticipacion().getId(), participacion.getId());
        java.math.BigDecimal notaOficial = null;
        if (seleccion != null) {
            notaOficial = resultados.findByParticipacionId(seleccion.getParticipacion().getId())
                    .map(Resultado::getNotaFinal)
                    .orElse(null);
        }
        return new ResultadoRespuesta(
                resultado.getId(),
                participacion.getId(),
                participacion.getNumeroIntento(),
                participacion.getEstudianteId(),
                estudiante != null ? estudiante.correo() : null,
                estudiante != null ? estudiante.nombreCompleto() : null,
                participacion.getPracticaId(),
                practica != null ? practica.nombre() : null,
                participacion.getEstado(),
                resultado.getCorrectas(),
                resultado.getIncorrectas(),
                resultado.getNoRespondidas(),
                resultado.getPesoObtenido(),
                resultado.getPesoTotal(),
                resultado.getEscalaMaxima(),
                resultado.getNotaFinal(),
                oficial,
                notaOficial,
                resultado.getFechaCalculo(),
                detalle(participacion, practica));
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

    private List<DetallePreguntaResultado> detalle(Participacion participacion, PracticaRef practica) {
        if (practica == null) {
            return List.of();
        }
        Map<Long, Long> elegidas = respuestas.findByParticipacionId(participacion.getId()).stream()
                .collect(Collectors.toMap(RespuestaSeleccionada::getPreguntaId, RespuestaSeleccionada::getRespuestaId));
        List<DetallePreguntaResultado> out = new ArrayList<>();
        for (PreguntaRef pregunta : catalogo.preguntasDelCaso(practica.casoId())) {
            Long respuestaId = elegidas.get(pregunta.id());
            boolean correcta = false;
            if (respuestaId != null) {
                correcta = catalogo.buscarRespuesta(respuestaId)
                        .map(RespuestaOpcionRef::correcta)
                        .orElse(false);
            }
            out.add(new DetallePreguntaResultado(
                    pregunta.id(),
                    pregunta.enunciado(),
                    pregunta.peso(),
                    respuestaId != null,
                    respuestaId,
                    correcta));
        }
        return out;
    }
}
