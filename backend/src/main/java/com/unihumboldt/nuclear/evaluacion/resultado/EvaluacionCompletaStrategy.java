package com.unihumboldt.nuclear.evaluacion.resultado;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

public class EvaluacionCompletaStrategy implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;
    private final RespuestaSeleccionadaRepository respuestas;

    public EvaluacionCompletaStrategy(CatalogoAcademico catalogo, RespuestaSeleccionadaRepository respuestas) {
        this.catalogo = catalogo;
        this.respuestas = respuestas;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.porRespuestas(participacion, catalogo, respuestas);
    }
}
