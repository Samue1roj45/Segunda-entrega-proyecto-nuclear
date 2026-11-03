package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;

/**
 * Normaliza los textos que entran al dominio. Un borrador admite campos vacíos (se guardan como
 * nulos), pero nunca un valor que no cabe en su columna: eso no es estar incompleto, es ser inválido.
 */
final class TextoDominio {

    private TextoDominio() {
    }

    static String normalizar(String valor, int longitudMaxima, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > longitudMaxima) {
            throw new SolicitudInvalidaException(
                    "El campo '" + campo + "' no puede superar " + longitudMaxima + " caracteres.");
        }
        return limpio;
    }
}
