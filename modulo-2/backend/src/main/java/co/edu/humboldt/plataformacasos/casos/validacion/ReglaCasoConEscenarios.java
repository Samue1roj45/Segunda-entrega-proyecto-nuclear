package co.edu.humboldt.plataformacasos.casos.validacion;

/** OCL-03: el caso debe contener al menos un escenario. */
final class ReglaCasoConEscenarios extends ReglaOcl {

    ReglaCasoConEscenarios() {
        super("OCL-03", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        if (revision.caso().getEscenarios().isEmpty()) {
            incumple(revision, "El caso debe contener al menos un escenario.", Ubicacion.ESCENARIOS);
        }
    }
}
