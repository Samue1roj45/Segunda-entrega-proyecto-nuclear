package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

/** Solo se entrega al docente dueño del caso: revela si la opción es correcta. */
public record JustificacionDTO(Long idRespuesta, String codigo, Boolean esCorrecta, String justificacion) {

    public static JustificacionDTO de(Respuesta respuesta) {
        return new JustificacionDTO(respuesta.getId(), respuesta.getCodigo(), respuesta.getEsCorrecta(),
                respuesta.getJustificacion());
    }
}
