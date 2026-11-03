package com.unihumboldt.nuclear.evaluacion.resultado;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;

/** RN22: autorizado que nunca inició → nota 0. */
public class EvaluacionAusenciaStrategy implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;

    public EvaluacionAusenciaStrategy(CatalogoAcademico catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.ausencia(participacion, catalogo);
    }
}
