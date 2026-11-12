package co.edu.humboldt.plataformacasos.sesiones.dto;

import co.edu.humboldt.plataformacasos.sesiones.dominio.TipoHerramienta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/* #REQUISITO RF-28 · El docente creador del caso registra una herramienta profesional. */
public record RegistrarHerramientaRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull TipoHerramienta tipo,
        @Size(max = 500) String descripcion,
        @Size(max = 20000) String contenido,
        @Positive Integer orden) {
}
