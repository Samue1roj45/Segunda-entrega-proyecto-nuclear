package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a Pregunta. El peso lo define el módulo de casos; no se inventa aquí.
 */
public record PreguntaRef(Long id, Long casoId, String enunciado, int peso) {
}
