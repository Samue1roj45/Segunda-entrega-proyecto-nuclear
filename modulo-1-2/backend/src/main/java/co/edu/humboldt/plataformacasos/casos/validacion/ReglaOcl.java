package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

import java.util.stream.Stream;

/**
 * Eslabón de la cadena de validación de RF-09 (Chain of Responsibility), sobre la base compartida del
 * Módulo 1. RF-09 exige informar todas las reglas incumplidas, así que un eslabón no corta la cadena:
 * anota lo que encuentra en la {@link RevisionCaso} y deja continuar. La decisión de publicar se toma
 * al final, con el informe completo.
 */
abstract class ReglaOcl extends ValidadorBase<RevisionCaso> {

    private final String codigo;
    private final SeccionCaso seccion;

    ReglaOcl(String codigo, SeccionCaso seccion) {
        this.codigo = codigo;
        this.seccion = seccion;
    }

    @Override
    protected final ResultadoValidacion validarCondicion(RevisionCaso revision) {
        comprobar(revision);
        return ResultadoValidacion.exito();
    }

    protected abstract void comprobar(RevisionCaso revision);

    protected final void incumple(RevisionCaso revision, String mensaje, String ubicacion) {
        revision.registrar(new Incumplimiento(codigo, mensaje, seccion, ubicacion));
    }

    protected static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    protected static Stream<Pregunta> preguntasDe(CasoEstudio caso) {
        return caso.getEscenarios().stream().flatMap(escenario -> escenario.getPreguntas().stream());
    }

    protected static Stream<Respuesta> respuestasDe(CasoEstudio caso) {
        return preguntasDe(caso).flatMap(pregunta -> pregunta.getRespuestas().stream());
    }
}
