package co.edu.humboldt.plataformacasos.compartido.excepcion;

/** Datos de entrada inválidos: se responde 400 con el mensaje. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
