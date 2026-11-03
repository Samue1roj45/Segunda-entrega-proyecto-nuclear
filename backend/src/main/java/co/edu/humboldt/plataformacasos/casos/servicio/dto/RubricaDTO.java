package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;

import java.util.List;

public record RubricaDTO(Long id, String nombre, String descripcion, List<CriterioDTO> criteriosEvaluacion) {

    public static RubricaDTO de(Rubrica rubrica) {
        return new RubricaDTO(rubrica.getId(), rubrica.getNombre(), rubrica.getDescripcion(),
                rubrica.getCriterios().stream().map(CriterioDTO::de).toList());
    }
}
