package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import java.util.List;

/** Caso creado desde un documento DSL-CEPS y lo que no se pudo incorporar. */
public record ResultadoImportacionDTO(CasoDetalleDTO caso, List<String> advertencias) {
}
