package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;
import co.edu.humboldt.plataformacasos.casos.dominio.LimitesDsl;
import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;

import java.util.List;

/** OCL-12: el criterio de evaluación no puede estar vacío ni superar 150 caracteres. */
final class ReglaCriterioValido extends ReglaOcl {

    ReglaCriterioValido() {
        super("OCL-12", SeccionCaso.RUBRICA);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        Rubrica rubrica = revision.caso().getRubrica();
        if (rubrica == null) {
            return; // Lo informa OCL-09.
        }
        List<CriterioEvaluacion> criterios = rubrica.getCriterios();
        for (int i = 0; i < criterios.size(); i++) {
            String criterio = criterios.get(i).getCriterio();
            if (vacio(criterio) || criterio.length() > LimitesDsl.LONGITUD_CRITERIO) {
                incumple(revision, "El criterio de evaluación no puede estar vacío ni superar "
                        + LimitesDsl.LONGITUD_CRITERIO + " caracteres.", Ubicacion.criterio(i + 1));
            }
        }
    }
}
