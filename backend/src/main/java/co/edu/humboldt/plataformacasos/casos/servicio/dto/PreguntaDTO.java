package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;

public record PreguntaDTO(Long id, String enunciado, int orden) {

    public static PreguntaDTO de(Pregunta pregunta) {
        return new PreguntaDTO(pregunta.getId(), pregunta.getEnunciado(), pregunta.getOrden());
    }
}
