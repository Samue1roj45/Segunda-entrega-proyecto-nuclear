package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a Rúbrica. escalaMaxima proviene del caso; si falta, la evaluación usa 100.
 */
public record RubricaRef(Long id, Long casoId, int escalaMaxima) {
}
