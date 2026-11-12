package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PreguntaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.RespuestaOpcionRef;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.EstadoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RespuestaSeleccionada;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RespuestaSeleccionadaRepositorio;

/**
 * Arma la respuesta de un resultado ya cerrado. No calcula ni elige la nota oficial.
 */
@Service
public class LecturaResultados {

    private final ResultadoRepositorio resultados;
    private final NotaOficialRepositorio notasOficiales;
    private final RespuestaSeleccionadaRepositorio respuestas;
    private final CatalogoAcademico catalogo;

    public LecturaResultados(
            ResultadoRepositorio resultados,
            NotaOficialRepositorio notasOficiales,
            RespuestaSeleccionadaRepositorio respuestas,
            CatalogoAcademico catalogo) {
        this.resultados = resultados;
        this.notasOficiales = notasOficiales;
        this.respuestas = respuestas;
        this.catalogo = catalogo;
    }

    @Transactional(readOnly = true)
    public Resultado exigir(Long id) {
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
                .filter(r -> r.getParticipacion().getEstado() != EstadoParticipacion.EN_CURSO)
                .filter(r -> practicaId == null || Objects.equals(r.getParticipacion().getPracticaId(), practicaId))
                .filter(r -> estudianteId == null || Objects.equals(r.getParticipacion().getEstudianteId(), estudianteId))
                .map(r -> toRespuesta(r, oficiales))
                .toList();
    }

    private Map<String, NotaOficial> mapaOficiales() {
        return notasOficiales.findAllByOrderByIdDesc().stream()
                .collect(Collectors.toMap(
                        n -> n.getPracticaId() + ":" + n.getEstudianteId(),
                        n -> n,
                        (a, b) -> a));
    }

    private ResultadoRespuesta toRespuesta(Resultado resultado, Map<String, NotaOficial> oficiales) {
        Participacion participacion = resultado.getParticipacion();
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        EstudianteRef estudiante = catalogo.buscarEstudiante(participacion.getEstudianteId()).orElse(null);
        NotaOficial seleccion = oficiales.get(participacion.getPracticaId() + ":" + participacion.getEstudianteId());
        boolean oficial = seleccion != null
                && Objects.equals(seleccion.getParticipacion().getId(), participacion.getId());
        BigDecimal notaOficial = null;
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
