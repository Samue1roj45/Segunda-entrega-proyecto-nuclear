package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RespuestaSeleccionadaRepositorio;

public class EstrategiaEvaluacionCompleta implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;
    private final RespuestaSeleccionadaRepositorio respuestas;

    public EstrategiaEvaluacionCompleta(CatalogoAcademico catalogo, RespuestaSeleccionadaRepositorio respuestas) {
        this.catalogo = catalogo;
        this.respuestas = respuestas;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.porRespuestas(participacion, catalogo, respuestas);
    }
}
