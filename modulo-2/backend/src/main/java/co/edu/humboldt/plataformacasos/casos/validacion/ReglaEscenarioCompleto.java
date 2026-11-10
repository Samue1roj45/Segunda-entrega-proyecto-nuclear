package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;

/** OCL-04: cada escenario debe tener contexto y al menos una pregunta. */
final class ReglaEscenarioCompleto extends ReglaOcl {

    ReglaEscenarioCompleto() {
        super("OCL-04", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        for (Escenario escenario : revision.caso().getEscenarios()) {
            if (vacio(escenario.getContexto())) {
                incumple(revision, "El escenario debe tener un contexto situacional.", Ubicacion.de(escenario));
            }
            if (escenario.getPreguntas().isEmpty()) {
                incumple(revision, "El escenario debe tener al menos una pregunta.", Ubicacion.de(escenario));
            }
        }
    }
}
