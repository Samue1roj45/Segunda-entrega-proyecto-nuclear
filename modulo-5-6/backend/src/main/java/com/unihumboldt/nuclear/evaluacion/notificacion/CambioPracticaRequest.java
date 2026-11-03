package com.unihumboldt.nuclear.evaluacion.notificacion;

import jakarta.validation.constraints.NotBlank;

public record CambioPracticaRequest(@NotBlank String motivo) {
}
