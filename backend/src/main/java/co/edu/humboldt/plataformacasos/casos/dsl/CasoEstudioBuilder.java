package co.edu.humboldt.plataformacasos.casos.dsl;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;

import java.math.BigDecimal;
import java.util.List;

/**
 * Patrón Builder (RF-05 a RF-08, RF-17): arma paso a paso un caso con sus escenarios, preguntas,
 * opciones y rúbrica. Cada paso se aplica al último elemento agregado. Construir no es validar: el
 * resultado siempre es un BORRADOR y las reglas de publicación las evalúa el validador DSL-CEPS.
 */
public final class CasoEstudioBuilder {

    private final CasoEstudio caso;
    private Escenario escenarioActual;
    private Pregunta preguntaActual;
    private Rubrica rubricaActual;

    private CasoEstudioBuilder(CasoEstudio caso) {
        this.caso = caso;
    }

    public static CasoEstudioBuilder nuevoCaso(Long docenteId, String nombre, String desarrolloSituacional,
                                               String contextoHistoria, Integer tiempoEstimadoMinutos) {
        return new CasoEstudioBuilder(CasoEstudio.nuevoBorrador(docenteId, nombre, desarrolloSituacional,
                contextoHistoria, tiempoEstimadoMinutos));
    }

    public CasoEstudioBuilder escenario(String nombre, String contexto) {
        escenarioActual = caso.agregarEscenario(nombre, contexto);
        preguntaActual = null;
        return this;
    }

    public CasoEstudioBuilder pregunta(String enunciado) {
        if (escenarioActual == null) {
            throw new IllegalStateException("Debe agregar un escenario antes de la pregunta");
        }
        preguntaActual = escenarioActual.agregarPregunta(enunciado);
        return this;
    }

    public CasoEstudioBuilder respuesta(String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                                        String justificacion) {
        if (preguntaActual == null) {
            throw new IllegalStateException("Debe agregar una pregunta antes de la respuesta");
        }
        preguntaActual.agregarRespuesta(codigo, texto, esCorrecta, porcentaje, justificacion);
        return this;
    }

    public CasoEstudioBuilder rubrica(String nombre, String descripcion) {
        rubricaActual = caso.registrarRubrica(nombre, descripcion);
        return this;
    }

    public CasoEstudioBuilder criterio(String criterio, List<String> parametros, BigDecimal min, BigDecimal max) {
        if (rubricaActual == null) {
            throw new IllegalStateException("Debe agregar la rúbrica antes del criterio");
        }
        rubricaActual.agregarCriterio(criterio, parametros, min, max);
        return this;
    }

    public CasoEstudio construir() {
        return caso;
    }
}
