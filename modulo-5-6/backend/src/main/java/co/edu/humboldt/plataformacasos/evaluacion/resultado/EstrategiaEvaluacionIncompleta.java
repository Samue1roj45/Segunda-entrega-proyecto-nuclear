package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RespuestaSeleccionadaRepositorio;

// Patrón Strategy: estrategia concreta para intentos incompletos.
/** RN21: califica el avance; las no respondidas no aportan peso obtenido. */
public class EstrategiaEvaluacionIncompleta implements EstrategiaEvaluacion {

    private final CatalogoAcademico catalogo;
    private final RespuestaSeleccionadaRepositorio respuestas;

    public EstrategiaEvaluacionIncompleta(CatalogoAcademico catalogo, RespuestaSeleccionadaRepositorio respuestas) {
        this.catalogo = catalogo;
        this.respuestas = respuestas;
    }

    @Override
    public MetricasEvaluacion calcular(Participacion participacion) {
        return CalculoRubrica.porRespuestas(participacion, catalogo, respuestas);
    }
}
