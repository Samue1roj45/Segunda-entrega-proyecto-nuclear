package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarRetroalimentacionRequest(
        @NotNull Long participacionId,
        @NotBlank String texto) {
}
