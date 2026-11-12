package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;

// Patrón Strategy: estrategia concreta para estudiantes que no participaron.
/** RN22: autorizado que nunca inició → nota 0. */
public class EstrategiaEvaluacionAusencia implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;

    public EstrategiaEvaluacionAusencia(CatalogoAcademico catalogo) {
        this.catalogo = catalogo;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.ausencia(participacion, catalogo);
    }
}
