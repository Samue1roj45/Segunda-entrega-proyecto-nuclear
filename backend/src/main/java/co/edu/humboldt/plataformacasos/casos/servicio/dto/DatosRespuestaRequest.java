package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import java.math.BigDecimal;

/** Opción de respuesta (RF-07, RF-08). {@code codigo} es el identificador del DSL (A, B, C...). */
public record DatosRespuestaRequest(String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                                    String justificacion) {
}
