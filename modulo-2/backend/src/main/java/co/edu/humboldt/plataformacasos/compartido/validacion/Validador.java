package co.edu.humboldt.plataformacasos.compartido.validacion;

/**
 * Patrón Chain of Responsibility: un eslabón de una cadena de validaciones.
 * Genérico para que lo reutilicen RF-01 (autenticación), RF-09 (publicación),
 * RF-20, RF-22 y RF-25 (participación) sin duplicar la mecánica de la cadena.
 *
 * @param <T> tipo de solicitud que se valida
 */
public interface Validador<T> {

    /**
     * Enlaza el siguiente eslabón y lo devuelve, para encadenar en una sola expresión:
     * {@code a.establecerSiguiente(b).establecerSiguiente(c)}.
     */
    Validador<T> establecerSiguiente(Validador<T> siguiente);

    ResultadoValidacion validar(T solicitud);
}
