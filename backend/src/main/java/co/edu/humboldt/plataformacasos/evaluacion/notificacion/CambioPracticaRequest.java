package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import jakarta.validation.constraints.NotBlank;

public record CambioPracticaRequest(@NotBlank String motivo) {
}
