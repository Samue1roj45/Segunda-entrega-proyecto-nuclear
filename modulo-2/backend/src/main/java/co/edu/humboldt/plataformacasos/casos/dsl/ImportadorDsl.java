package co.edu.humboldt.plataformacasos.casos.dsl;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.CasoDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.CriterioDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.EscenarioDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.PreguntaDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.RespuestaDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.RubricaDsl;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Director del Builder: recorre un documento DSL-CEPS y arma con él un caso en BORRADOR. */
@Component
public class ImportadorDsl {

    public CasoEstudio construir(Long docenteId, DocumentoDsl documento) {
        exigirFormato(documento);
        CasoDsl datos = documento.caso();
        CasoEstudioBuilder builder = CasoEstudioBuilder.nuevoCaso(docenteId, datos.nombre(),
                datos.desarrolloSituacional(), datos.contextoHistoria(), datos.tiempoEstimadoMinutos());
        elementos(datos.escenarios()).forEach(escenario -> agregarEscenario(builder, escenario));
        if (datos.rubrica() != null) {
            agregarRubrica(builder, datos.rubrica());
        }
        return builder.construir();
    }

    private static void exigirFormato(DocumentoDsl documento) {
        if (!DocumentoDsl.NOMBRE.equals(documento.dsl()) || !DocumentoDsl.VERSION.equals(documento.version())) {
            throw new SolicitudInvalidaException("El documento debe declarar dsl '" + DocumentoDsl.NOMBRE
                    + "' y version '" + DocumentoDsl.VERSION + "'.");
        }
        if (documento.caso() == null) {
            throw new SolicitudInvalidaException("El documento no contiene el objeto 'caso'.");
        }
    }

    private static void agregarEscenario(CasoEstudioBuilder builder, EscenarioDsl escenario) {
        builder.escenario(escenario.nombre(), escenario.contexto());
        elementos(escenario.preguntas()).forEach(pregunta -> agregarPregunta(builder, pregunta));
    }

    private static void agregarPregunta(CasoEstudioBuilder builder, PreguntaDsl pregunta) {
        builder.pregunta(pregunta.enunciado());
        for (RespuestaDsl respuesta : elementos(pregunta.respuestas())) {
            builder.respuesta(respuesta.id(), respuesta.texto(), respuesta.esCorrecta(), respuesta.porcentaje(),
                    respuesta.justificacion());
        }
    }

    private static void agregarRubrica(CasoEstudioBuilder builder, RubricaDsl rubrica) {
        builder.rubrica(rubrica.nombre(), rubrica.descripcion());
        for (CriterioDsl criterio : elementos(rubrica.criteriosEvaluacion())) {
            BigDecimal min = criterio.rango() == null ? null : criterio.rango().min();
            BigDecimal max = criterio.rango() == null ? null : criterio.rango().max();
            builder.criterio(criterio.criterio(), criterio.parametros(), min, max);
        }
    }

    /** Un JSON puede omitir una lista o traer elementos nulos; no son errores de un borrador. */
    private static <T> List<T> elementos(List<T> lista) {
        return lista == null ? List.of() : lista.stream().filter(Objects::nonNull).toList();
    }
}
