package co.edu.humboldt.plataformacasos.sesiones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/*
 #REQUISITO RF-18 (y RF-21: los convocados reciben el aviso del cambio)
 Para qué sirve: nuevas condiciones de una sesión que aún no ha finalizado.
 */
public record ModificarSesionRequest(
        @NotBlank @Size(max = 150) String espacioAcademico,
        @NotNull LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin,
        @NotNull @Positive Integer tiempoMaximoMinutos,
        @Size(max = 150) String lugar,
        String mensajePersonalizado,
        @Size(max = 255) String urlAcceso) {
}
