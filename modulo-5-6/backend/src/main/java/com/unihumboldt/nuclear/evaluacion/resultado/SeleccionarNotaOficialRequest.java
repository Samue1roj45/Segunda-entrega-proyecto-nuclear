package com.unihumboldt.nuclear.evaluacion.resultado;

import jakarta.validation.constraints.NotNull;

public record SeleccionarNotaOficialRequest(
        @NotNull Long practicaId,
        @NotNull Long estudianteId,
        @NotNull Long participacionId) {
}
