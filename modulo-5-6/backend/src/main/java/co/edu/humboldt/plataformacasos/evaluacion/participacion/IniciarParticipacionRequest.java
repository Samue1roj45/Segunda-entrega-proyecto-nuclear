package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import jakarta.validation.constraints.NotNull;

public record IniciarParticipacionRequest(@NotNull Long autorizacionId) {
}
