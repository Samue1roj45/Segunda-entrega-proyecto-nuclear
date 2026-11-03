package com.unihumboldt.nuclear.evaluacion.contrato;

import java.time.OffsetDateTime;

/**
 * Referencia temporal a Práctica. Fechas usadas por RF-12 (RN17) y RF-21.
 */
public record PracticaRef(
        Long id,
        String nombre,
        Long casoId,
        Long docenteId,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String estado,
        String mensajePersonalizado) {
}
