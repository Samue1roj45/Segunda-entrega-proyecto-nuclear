package com.unihumboldt.nuclear.evaluacion.participacion;

import jakarta.validation.constraints.NotNull;

public record RegistrarRespuestaRequest(@NotNull Long preguntaId, @NotNull Long respuestaId) {
}
