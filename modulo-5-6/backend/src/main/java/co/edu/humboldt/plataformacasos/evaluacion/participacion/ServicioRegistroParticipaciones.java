package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PreguntaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.RespuestaOpcionRef;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.CalculoNota;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Implementación del puerto {@link RegistroParticipaciones} sobre las entidades de este módulo. */
@Service
public class ServicioRegistroParticipaciones implements RegistroParticipaciones {

    private final ParticipacionRepositorio participaciones;
    private final RespuestaSeleccionadaRepositorio respuestas;
    private final CatalogoAcademico catalogo;
    private final CalculoNota calculo;
    private final Clock clock;

    public ServicioRegistroParticipaciones(ParticipacionRepositorio participaciones,
                                           RespuestaSeleccionadaRepositorio respuestas,
                                           CatalogoAcademico catalogo, CalculoNota calculo, Clock clock) {
        this.participaciones = participaciones;
        this.respuestas = respuestas;
        this.catalogo = catalogo;
        this.calculo = calculo;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IntentoParticipacion> intento(Long participacionId) {
        return participaciones.findById(participacionId).map(IntentoParticipacion::de);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntentoParticipacion> intentosDeAutorizacion(Long autorizacionId) {
        return participaciones.findByAutorizacionIdOrderByNumeroIntentoAsc(autorizacionId).stream()
                .map(IntentoParticipacion::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntentoParticipacion> intentosEnCurso() {
        return participaciones.findByEstado(EstadoParticipacion.EN_CURSO).stream()
                .map(IntentoParticipacion::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> respuestasSeleccionadas(Long participacionId) {
        return respuestas.findByParticipacionId(participacionId).stream()
                .collect(Collectors.toMap(RespuestaSeleccionada::getPreguntaId, RespuestaSeleccionada::getRespuestaId));
    }

    @Override
    @Transactional
    public void registrarRespuesta(Long participacionId, Long preguntaId, Long respuestaId) {
        Participacion participacion = participaciones.findById(participacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada."));
        if (participacion.getEstado() != EstadoParticipacion.EN_CURSO) {
            throw new ReglaNegocioException("No es posible alterar respuestas de un intento cerrado.");
        }
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));
        PreguntaRef pregunta = catalogo.preguntasDelCaso(practica.casoId()).stream()
                .filter(p -> p.id().equals(preguntaId))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La pregunta no pertenece al caso de esta práctica."));
        RespuestaOpcionRef opcion = catalogo.buscarRespuesta(respuestaId)
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

    @Override
    @Transactional
    public IntentoParticipacion cerrarPorTiempo(Long participacionId) {
        Participacion participacion = participaciones.findById(participacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada."));
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        int totalPreguntas = catalogo.buscarPractica(participacion.getPracticaId())
                .map(p -> catalogo.preguntasDelCaso(p.casoId()).size())
                .orElse(0);
        boolean respondioTodo = respuestas.findByParticipacionId(participacionId).size() >= totalPreguntas;
        if (respondioTodo) {
            MaquinaEstadoParticipacion.finalizar(participacion, ahora);
        } else {
            MaquinaEstadoParticipacion.marcarIncompleta(participacion, ahora);
        }
        participacion = participaciones.save(participacion);
        calculo.calcular(participacion);
        return IntentoParticipacion.de(participacion);
    }
}
