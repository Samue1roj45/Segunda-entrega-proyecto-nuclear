package co.edu.humboldt.plataformacasos.casos.validacion;

/**
 * OCL-09: un caso que no está en Borrador debe tener una rúbrica. La cadena se evalúa sobre el caso
 * candidato a publicarse, por lo que se exige aunque su estado actual todavía sea BORRADOR.
 */
final class ReglaRubricaAsociada extends ReglaOcl {

    ReglaRubricaAsociada() {
        super("OCL-09", SeccionCaso.RUBRICA);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        if (revision.caso().getRubrica() == null) {
            incumple(revision, "El caso debe tener una rúbrica asociada para publicarse.", Ubicacion.RUBRICA);
        }
    }
}
