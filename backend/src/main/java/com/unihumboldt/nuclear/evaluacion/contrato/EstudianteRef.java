package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a Estudiante (módulo académico ajeno).
 */
public record EstudianteRef(Long id, String correo, String nombreCompleto, Long usuarioId) {
}
