package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;

// Patrón Strategy: contrato para calcular el resultado según el tipo de participación.
public interface EstrategiaEvaluacion {

    MetricasEvaluacion calcular(Participacion participacion);
}
