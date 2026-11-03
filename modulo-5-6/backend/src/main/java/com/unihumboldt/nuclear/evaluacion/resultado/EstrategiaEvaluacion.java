package com.unihumboldt.nuclear.evaluacion.resultado;

import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;

public interface EstrategiaEvaluacion {

    MetricasEvaluacion calcular(Participacion participacion);
}
