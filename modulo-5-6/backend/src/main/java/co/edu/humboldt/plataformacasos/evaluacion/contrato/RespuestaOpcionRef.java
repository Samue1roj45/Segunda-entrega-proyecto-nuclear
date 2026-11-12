package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Opción de respuesta de una pregunta del caso (módulo 2) y si es la correcta.
 */
public record RespuestaOpcionRef(Long id, Long preguntaId, String texto, boolean correcta) {
}
