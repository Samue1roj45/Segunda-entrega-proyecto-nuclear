package co.edu.humboldt.plataformacasos.avisos;

import java.time.LocalDateTime;

public record EntradaAuditoriaDto(Long id, LocalDateTime fecha, String actorCorreo, String accion, String detalle) {
}
