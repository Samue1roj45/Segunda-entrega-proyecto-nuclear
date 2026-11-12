package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.LimitesDsl;

import java.math.BigDecimal;

/** OCL-11: el porcentaje de cada respuesta debe estar entre 0 y 100. */
final class ReglaPorcentajeRespuesta extends ReglaOcl {

    ReglaPorcentajeRespuesta() {
        super("OCL-11", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        respuestasDe(revision.caso())
                .filter(respuesta -> !enRango(respuesta.getPorcentaje()))
                .forEach(respuesta -> incumple(revision, "El porcentaje de la opción debe estar entre "
                        + LimitesDsl.PORCENTAJE_MINIMO + " y " + LimitesDsl.PORCENTAJE_MAXIMO + ".",
                        Ubicacion.de(respuesta)));
    }

    private static boolean enRango(BigDecimal porcentaje) {
        return porcentaje != null && porcentaje.compareTo(LimitesDsl.PORCENTAJE_MINIMO) >= 0
                && porcentaje.compareTo(LimitesDsl.PORCENTAJE_MAXIMO) <= 0;
    }
}
