package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a una opción de respuesta del caso.
 */
public record RespuestaOpcionRef(Long id, Long preguntaId, String texto, boolean correcta) {
}
