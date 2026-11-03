package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import java.math.BigDecimal;

/** Rango de evaluación de un criterio. Agrupa min y max igual que la sintaxis JSON del DSL-CEPS. */
public record RangoDTO(BigDecimal min, BigDecimal max) {
}
