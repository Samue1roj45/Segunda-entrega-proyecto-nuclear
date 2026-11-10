package co.edu.humboldt.plataformacasos.casos.dsl;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;
import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;
import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RangoDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sintaxis concreta del DSL-CEPS (Figura 3). Describe solo el contenido académico: el estado del
 * caso lo gestiona el sistema y por eso no aparece aquí. El {@code id} de una respuesta es su
 * identificador del DSL (A, B, C...), no la clave de la base de datos.
 */
public record DocumentoDsl(String dsl, String version, CasoDsl caso) {

    public static final String NOMBRE = "DSL-CEPS";
    public static final String VERSION = "1.1";

    public record CasoDsl(String nombre, String desarrolloSituacional, String contextoHistoria,
                          Integer tiempoEstimadoMinutos, List<EscenarioDsl> escenarios, RubricaDsl rubrica,
                          List<Object> herramientas) {
    }

    public record EscenarioDsl(String nombre, String contexto, List<PreguntaDsl> preguntas) {
    }

    public record PreguntaDsl(String enunciado, List<RespuestaDsl> respuestas) {
    }

    public record RespuestaDsl(String id, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                               String justificacion) {
    }

    public record RubricaDsl(String nombre, String descripcion, List<CriterioDsl> criteriosEvaluacion) {
    }

    public record CriterioDsl(String criterio, List<String> parametros, RangoDTO rango) {
    }

    public static DocumentoDsl de(CasoEstudio caso) {
        return new DocumentoDsl(NOMBRE, VERSION, new CasoDsl(caso.getNombre(), caso.getDesarrolloSituacional(),
                caso.getContextoHistoria(), caso.getTiempoEstimadoMinutos(),
                caso.getEscenarios().stream().map(DocumentoDsl::escenario).toList(),
                caso.getRubrica() == null ? null : rubrica(caso.getRubrica()),
                // RF-28 no pertenece a este módulo: el caso aún no tiene herramientas que exportar.
                List.of()));
    }

    private static EscenarioDsl escenario(Escenario escenario) {
        return new EscenarioDsl(escenario.getNombre(), escenario.getContexto(),
                escenario.getPreguntas().stream().map(DocumentoDsl::pregunta).toList());
    }

    private static PreguntaDsl pregunta(Pregunta pregunta) {
        return new PreguntaDsl(pregunta.getEnunciado(),
                pregunta.getRespuestas().stream().map(DocumentoDsl::respuesta).toList());
    }

    private static RespuestaDsl respuesta(Respuesta respuesta) {
        return new RespuestaDsl(respuesta.getCodigo(), respuesta.getTexto(), respuesta.getEsCorrecta(),
                respuesta.getPorcentaje(), respuesta.getJustificacion());
    }

    private static RubricaDsl rubrica(Rubrica rubrica) {
        return new RubricaDsl(rubrica.getNombre(), rubrica.getDescripcion(),
                rubrica.getCriterios().stream().map(DocumentoDsl::criterio).toList());
    }

    private static CriterioDsl criterio(CriterioEvaluacion criterio) {
        return new CriterioDsl(criterio.getCriterio(), criterio.getParametros(),
                new RangoDTO(criterio.getRangoMin(), criterio.getRangoMax()));
    }
}
