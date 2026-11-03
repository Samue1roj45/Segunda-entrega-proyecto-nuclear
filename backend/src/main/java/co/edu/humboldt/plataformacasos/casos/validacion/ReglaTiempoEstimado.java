package co.edu.humboldt.plataformacasos.casos.validacion;

/** OCL-02: el tiempo estimado debe ser positivo. */
final class ReglaTiempoEstimado extends ReglaOcl {

    ReglaTiempoEstimado() {
        super("OCL-02", SeccionCaso.DATOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        Integer minutos = revision.caso().getTiempoEstimadoMinutos();
        if (minutos == null || minutos <= 0) {
            incumple(revision, "El tiempo estimado debe ser un número positivo de minutos.",
                    Ubicacion.DATOS_DEL_CASO);
        }
    }
}
