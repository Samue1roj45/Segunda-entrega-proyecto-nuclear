package com.unihumboldt.nuclear.evaluacion.resultado;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

/** RN21: califica el avance; las no respondidas no aportan peso obtenido. */
public class EvaluacionIncompletaStrategy implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;
    private final RespuestaSeleccionadaRepository respuestas;

    public EvaluacionIncompletaStrategy(CatalogoAcademico catalogo, RespuestaSeleccionadaRepository respuestas) {
        this.catalogo = catalogo;
        this.respuestas = respuestas;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.porRespuestas(participacion, catalogo, respuestas);
    }
}
