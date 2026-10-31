package com.unihumboldt.nuclear.evaluacion.resultado;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.PreguntaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RespuestaOpcionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RubricaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionada;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

/**
 * Fórmula oficial: nota = (peso_obtenido / peso_total) * rubrica.escala_maxima.
 * Si no hay rúbrica, escala = 100. Preguntas sin respuesta no suman peso obtenido.
 */
public final class CalculoRubrica {

    private CalculoRubrica() {
    }

    public static int escala(CatalogoAcademico catalogo, Long casoId) {
        return catalogo.buscarRubricaPorCaso(casoId)
                .map(RubricaRef::escalaMaxima)
                .orElse(100);
    }

    public static MetricasEvaluacion porRespuestas(
            Participacion participacion,
            CatalogoAcademico catalogo,
            RespuestaSeleccionadaRepository respuestas) {
        Long casoId = catalogo.buscarPractica(participacion.getPracticaId())
                .map(p -> p.casoId())
                .orElseThrow(() -> new ReglaNegocioException("No hay práctica/caso para calcular la nota."));
        List<PreguntaRef> preguntas = catalogo.preguntasDelCaso(casoId);
        Map<Long, Long> elegidas = respuestas.findByParticipacionId(participacion.getId()).stream()
                .collect(Collectors.toMap(RespuestaSeleccionada::getPreguntaId, RespuestaSeleccionada::getRespuestaId));

        int correctas = 0;
        int incorrectas = 0;
        int noRespondidas = 0;
        int pesoObtenido = 0;
        int pesoTotal = preguntas.stream().mapToInt(PreguntaRef::peso).sum();

        for (PreguntaRef pregunta : preguntas) {
            Long respuestaId = elegidas.get(pregunta.id());
            if (respuestaId == null) {
                noRespondidas++;
                continue;
            }
            RespuestaOpcionRef opcion = catalogo.buscarRespuesta(respuestaId).orElse(null);
            if (opcion != null && opcion.correcta() && opcion.preguntaId().equals(pregunta.id())) {
                correctas++;
                pesoObtenido += pregunta.peso();
            } else {
                incorrectas++;
            }
        }

        int escalaMaxima = escala(catalogo, casoId);
        BigDecimal nota;
        if (pesoTotal > 0) {
            nota = BigDecimal.valueOf(pesoObtenido)
                    .multiply(BigDecimal.valueOf(escalaMaxima))
                    .divide(BigDecimal.valueOf(pesoTotal), 2, RoundingMode.HALF_UP);
        } else {
            nota = BigDecimal.ZERO.setScale(2);
        }
        return new MetricasEvaluacion(
                correctas, incorrectas, noRespondidas, pesoObtenido, pesoTotal, escalaMaxima, nota);
    }

    public static MetricasEvaluacion ausencia(
            Participacion participacion,
            CatalogoAcademico catalogo) {
        Long casoId = catalogo.buscarPractica(participacion.getPracticaId())
                .map(p -> p.casoId())
                .orElse(null);
        List<PreguntaRef> preguntas = casoId == null ? List.of() : catalogo.preguntasDelCaso(casoId);
        int pesoTotal = preguntas.stream().mapToInt(PreguntaRef::peso).sum();
        int escalaMaxima = casoId == null ? 100 : escala(catalogo, casoId);
        return new MetricasEvaluacion(
                0, 0, preguntas.size(), 0, pesoTotal, escalaMaxima, BigDecimal.ZERO.setScale(2));
    }
}
