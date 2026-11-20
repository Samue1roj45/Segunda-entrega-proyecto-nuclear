package co.edu.humboldt.plataformacasos.avisos;

import java.time.LocalDateTime;

public record AvisoDto(Long id, String tipo, String titulo, String mensaje, boolean leido, LocalDateTime fecha) {
}
