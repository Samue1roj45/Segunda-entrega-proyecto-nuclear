package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.LimitesDsl;

/** OCL-05: cada pregunta debe tener al menos dos opciones de respuesta. */
final class ReglaMinimoRespuestas extends ReglaOcl {

    ReglaMinimoRespuestas() {
        super("OCL-05", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        preguntasDe(revision.caso())
                .filter(pregunta -> pregunta.getRespuestas().size() < LimitesDsl.MINIMO_RESPUESTAS_POR_PREGUNTA)
                .forEach(pregunta -> incumple(revision, "La pregunta debe tener al menos "
                        + LimitesDsl.MINIMO_RESPUESTAS_POR_PREGUNTA + " opciones de respuesta.",
                        Ubicacion.de(pregunta)));
    }
}
