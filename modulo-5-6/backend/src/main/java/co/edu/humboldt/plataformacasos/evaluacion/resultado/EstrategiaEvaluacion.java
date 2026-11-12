package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;

public interface EstrategiaEvaluacion {

    MetricasEvaluacion calcular(Participacion participacion);
}
