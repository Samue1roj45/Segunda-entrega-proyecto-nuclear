package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos del caso para calificar y retroalimentar (RF-13, RF-14, RF-24). Solo contiene datos: la
 * fórmula de la nota pertenece al Módulo 4. Nunca debe entregarse a un estudiante durante su intento.
 */
public record ClaveEvaluacionDTO(Long idCaso, List<PreguntaClave> preguntas, RubricaDTO rubrica) {

    public record PreguntaClave(Long id, String enunciado, List<OpcionClave> opciones) {
    }

    public record OpcionClave(Long id, String codigo, String texto, boolean esCorrecta, BigDecimal porcentaje,
                              String justificacion) {
    }

    static ClaveEvaluacionDTO de(CasoEstudio caso) {
        List<PreguntaClave> preguntas = caso.getEscenarios().stream()
                .flatMap(escenario -> escenario.getPreguntas().stream())
                .map(ClaveEvaluacionDTO::pregunta).toList();
        return new ClaveEvaluacionDTO(caso.getId(), preguntas, RubricaDTO.de(caso.rubricaRegistrada()));
    }

    private static PreguntaClave pregunta(Pregunta pregunta) {
        return new PreguntaClave(pregunta.getId(), pregunta.getEnunciado(),
                pregunta.getRespuestas().stream().map(ClaveEvaluacionDTO::opcion).toList());
    }

    private static OpcionClave opcion(Respuesta respuesta) {
        return new OpcionClave(respuesta.getId(), respuesta.getCodigo(), respuesta.getTexto(),
                Boolean.TRUE.equals(respuesta.getEsCorrecta()), respuesta.getPorcentaje(),
                respuesta.getJustificacion());
    }
}
