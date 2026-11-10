package co.edu.humboldt.plataformacasos.casos.validacion;

/** OCL-01: el nombre del caso no puede estar vacío. */
final class ReglaNombreCaso extends ReglaOcl {

    ReglaNombreCaso() {
        super("OCL-01", SeccionCaso.DATOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        if (vacio(revision.caso().getNombre())) {
            incumple(revision, "El nombre del caso no puede estar vacío.", Ubicacion.DATOS_DEL_CASO);
        }
    }
}
