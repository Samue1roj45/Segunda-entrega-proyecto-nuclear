package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Pregunta del caso (módulo 2). El módulo 2 no define pesos: todas las preguntas pesan 1.
 */
public record PreguntaRef(Long id, Long casoId, String enunciado, int peso) {
}
