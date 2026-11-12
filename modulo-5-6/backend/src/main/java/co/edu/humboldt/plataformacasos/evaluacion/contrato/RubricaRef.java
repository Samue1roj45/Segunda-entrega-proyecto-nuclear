package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Rúbrica del caso (módulo 2, RF-17). escalaMaxima es el mayor rango máximo de sus criterios;
 * si la rúbrica no tiene rangos, la evaluación usa 100.
 */
public record RubricaRef(Long id, Long casoId, int escalaMaxima) {
}
