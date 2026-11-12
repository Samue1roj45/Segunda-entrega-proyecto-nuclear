package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import java.math.BigDecimal;
import java.util.List;

/** Criterio de evaluación (RF-17). El rango puede faltar mientras el caso sea un borrador. */
public record DatosCriterioRequest(String criterio, List<String> parametros, RangoDTO rango) {

    public BigDecimal min() {
        return rango == null ? null : rango.min();
    }

    public BigDecimal max() {
        return rango == null ? null : rango.max();
    }
}
