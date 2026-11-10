package co.edu.humboldt.plataformacasos.compartido.excepcion;

/** El recurso solicitado no existe: se responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
