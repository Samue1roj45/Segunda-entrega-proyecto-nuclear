package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

/** OCL-06: cada respuesta debe declarar si es correcta y explicar por qué lo es o no lo es. */
final class ReglaRespuestaJustificada extends ReglaOcl {

    ReglaRespuestaJustificada() {
        super("OCL-06", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        respuestasDe(revision.caso()).forEach(respuesta -> comprobarRespuesta(revision, respuesta));
    }

    private void comprobarRespuesta(RevisionCaso revision, Respuesta respuesta) {
        if (respuesta.getEsCorrecta() == null) {
            incumple(revision, "La opción debe declarar si es correcta o incorrecta.", Ubicacion.de(respuesta));
        }
        if (vacio(respuesta.getJustificacion())) {
            incumple(revision, "La opción debe tener una justificación que explique por qué es correcta "
                    + "o incorrecta.", Ubicacion.de(respuesta));
        }
    }
}
