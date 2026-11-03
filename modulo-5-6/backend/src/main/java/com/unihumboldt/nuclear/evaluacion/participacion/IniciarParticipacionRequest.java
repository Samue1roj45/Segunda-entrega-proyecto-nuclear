package com.unihumboldt.nuclear.evaluacion.participacion;

import jakarta.validation.constraints.NotNull;

public record IniciarParticipacionRequest(@NotNull Long autorizacionId) {
}
