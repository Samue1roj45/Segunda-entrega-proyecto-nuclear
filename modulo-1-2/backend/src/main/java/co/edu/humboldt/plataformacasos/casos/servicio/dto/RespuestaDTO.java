package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

import java.math.BigDecimal;

/** Incluye la clave y la justificación: solo se entrega al docente dueño del caso. */
public record RespuestaDTO(Long id, String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                           String justificacion) {

    public static RespuestaDTO de(Respuesta respuesta) {
        return new RespuestaDTO(respuesta.getId(), respuesta.getCodigo(), respuesta.getTexto(),
                respuesta.getEsCorrecta(), respuesta.getPorcentaje(), respuesta.getJustificacion());
    }
}
