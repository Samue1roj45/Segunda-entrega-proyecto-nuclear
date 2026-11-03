package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

/** Describe en lenguaje del docente el lugar del caso donde está cada incumplimiento. */
final class Ubicacion {

    static final String DATOS_DEL_CASO = "Datos del caso";
    static final String ESCENARIOS = "Escenarios del caso";
    static final String RUBRICA = "Rúbrica";
    private static final String SEPARADOR = " › ";

    private Ubicacion() {
    }

    static String de(Escenario escenario) {
        String base = "Escenario " + escenario.getOrden();
        return escenario.getNombre() == null ? base : base + " «" + escenario.getNombre() + "»";
    }

    static String de(Pregunta pregunta) {
        return de(pregunta.getEscenario()) + SEPARADOR + "Pregunta " + pregunta.getOrden();
    }

    static String de(Respuesta respuesta) {
        String codigo = respuesta.getCodigo() == null ? "sin identificador" : respuesta.getCodigo();
        return de(respuesta.getPregunta()) + SEPARADOR + "Opción " + codigo;
    }

    static String criterio(int posicion) {
        return RUBRICA + SEPARADOR + "Criterio " + posicion;
    }
}
