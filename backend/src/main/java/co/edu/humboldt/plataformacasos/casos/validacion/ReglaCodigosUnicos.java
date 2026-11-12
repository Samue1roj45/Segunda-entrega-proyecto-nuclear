package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/** OCL-08: los identificadores de respuesta deben existir y ser únicos dentro de cada pregunta. */
final class ReglaCodigosUnicos extends ReglaOcl {

    ReglaCodigosUnicos() {
        super("OCL-08", SeccionCaso.ESCENARIOS);
    }

    @Override
    protected void comprobar(RevisionCaso revision) {
        preguntasDe(revision.caso()).forEach(pregunta -> comprobarPregunta(revision, pregunta));
    }

    private void comprobarPregunta(RevisionCaso revision, Pregunta pregunta) {
        Set<String> vistos = new HashSet<>();
        Set<String> repetidos = new TreeSet<>();
        boolean faltaAlguno = false;
        for (Respuesta respuesta : pregunta.getRespuestas()) {
            if (vacio(respuesta.getCodigo())) {
                faltaAlguno = true;
            } else if (!vistos.add(respuesta.getCodigo())) {
                repetidos.add(respuesta.getCodigo());
            }
        }
        if (faltaAlguno) {
            incumple(revision, "Todas las opciones de la pregunta deben tener un identificador.",
                    Ubicacion.de(pregunta));
        }
        if (!repetidos.isEmpty()) {
            incumple(revision, "Los identificadores de las opciones deben ser únicos en la pregunta. Repetidos: "
                    + String.join(", ", repetidos) + ".", Ubicacion.de(pregunta));
        }
    }
}
