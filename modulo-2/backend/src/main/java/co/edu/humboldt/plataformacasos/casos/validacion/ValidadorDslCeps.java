package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.compartido.validacion.Validador;
import org.springframework.stereotype.Component;

// Patrón Chain of Responsibility: recorre la lista de ReglaOcl y detiene la publicación en la primera violación.
/**
 * Validador DSL-CEPS de la Figura 7 (RF-09). Arma la cadena con las reglas OCL del contenido del
 * caso y entrega el informe completo. Agregar una regla es agregar un eslabón aquí.
 *
 * <p>OCL-13 (herramientas profesionales, RF-28) se incorporará como eslabón cuando exista ese
 * módulo; hoy el caso no tiene herramientas que revisar.</p>
 */
@Component
public class ValidadorDslCeps {

    private final Validador<RevisionCaso> cadena;

    public ValidadorDslCeps() {
        cadena = new ReglaNombreCaso();
        cadena.establecerSiguiente(new ReglaTiempoEstimado())
                .establecerSiguiente(new ReglaCasoConEscenarios())
                .establecerSiguiente(new ReglaEscenarioCompleto())
                .establecerSiguiente(new ReglaMinimoRespuestas())
                .establecerSiguiente(new ReglaRespuestaJustificada())
                .establecerSiguiente(new ReglaUnicaCorrecta())
                .establecerSiguiente(new ReglaCodigosUnicos())
                .establecerSiguiente(new ReglaRubricaAsociada())
                .establecerSiguiente(new ReglaRubricaConCriterios())
                .establecerSiguiente(new ReglaPorcentajeRespuesta())
                .establecerSiguiente(new ReglaCriterioValido());
    }

    public InformeValidacion validar(CasoEstudio caso) {
        RevisionCaso revision = new RevisionCaso(caso);
        cadena.validar(revision);
        return InformeValidacion.de(revision.incumplimientos());
    }
}
