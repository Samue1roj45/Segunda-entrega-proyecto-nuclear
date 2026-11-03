package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;

import java.util.List;

public record PreguntaDTO(Long id, String enunciado, int orden, List<RespuestaDTO> respuestas) {

    public static PreguntaDTO de(Pregunta pregunta) {
        return new PreguntaDTO(pregunta.getId(), pregunta.getEnunciado(), pregunta.getOrden(),
                pregunta.getRespuestas().stream().map(RespuestaDTO::de).toList());
    }
}
