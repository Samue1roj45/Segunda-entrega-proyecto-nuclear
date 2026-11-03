package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.LimitesDsl;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;

/** OCL-07: cada pregunta debe tener exactamente una respuesta correcta. */
final class ReglaUnicaCorrecta extends ReglaOcl {

    ReglaUnicaCorrecta() {
        super("OCL-07", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        preguntasDe(revision.caso()).forEach(pregunta -> {
            long correctas = contarCorrectas(pregunta);
            if (correctas != LimitesDsl.RESPUESTAS_CORRECTAS_POR_PREGUNTA) {
                incumple(revision, "La pregunta debe tener exactamente una respuesta correcta y tiene "
                        + correctas + ".", Ubicacion.de(pregunta));
            }
        });
    }

    private static long contarCorrectas(Pregunta pregunta) {
        return pregunta.getRespuestas().stream().filter(r -> Boolean.TRUE.equals(r.getEsCorrecta())).count();
    }
}
