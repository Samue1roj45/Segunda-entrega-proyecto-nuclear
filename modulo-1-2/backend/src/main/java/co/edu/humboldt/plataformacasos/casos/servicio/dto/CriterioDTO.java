package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;

import java.util.List;

public record CriterioDTO(Long id, String criterio, List<String> parametros, RangoDTO rango) {

    public static CriterioDTO de(CriterioEvaluacion criterio) {
        return new CriterioDTO(criterio.getId(), criterio.getCriterio(), criterio.getParametros(),
                new RangoDTO(criterio.getRangoMin(), criterio.getRangoMax()));
    }
}
