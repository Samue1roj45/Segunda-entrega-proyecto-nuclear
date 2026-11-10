package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;
import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;

import java.util.List;

/** OCL-10: la rúbrica debe tener al menos un criterio y cada criterio debe cumplir min &lt; max. */
final class ReglaRubricaConCriterios extends ReglaOcl {

    ReglaRubricaConCriterios() {
        super("OCL-10", SeccionCaso.RUBRICA);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        Rubrica rubrica = revision.caso().getRubrica();
        if (rubrica == null) {
            return; // Lo informa OCL-09.
        }
        List<CriterioEvaluacion> criterios = rubrica.getCriterios();
        if (criterios.isEmpty()) {
            incumple(revision, "La rúbrica debe tener al menos un criterio de evaluación.", Ubicacion.RUBRICA);
        }
        for (int i = 0; i < criterios.size(); i++) {
            if (!rangoValido(criterios.get(i))) {
                incumple(revision, "El rango mínimo del criterio debe ser menor que el máximo.",
                        Ubicacion.criterio(i + 1));
            }
        }
    }

    private static boolean rangoValido(CriterioEvaluacion criterio) {
        return criterio.getRangoMin() != null && criterio.getRangoMax() != null
                && criterio.getRangoMin().compareTo(criterio.getRangoMax()) < 0;
    }
}
