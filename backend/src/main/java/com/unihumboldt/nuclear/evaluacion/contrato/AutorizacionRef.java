package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Referencia temporal a AutorizacionEstudiante (códigos: módulo de prácticas).
 */
public record AutorizacionRef(
        Long id,
        Long practicaId,
        Long estudianteId,
        String codigoAcceso,
        boolean reintentoAutorizado) {
}
