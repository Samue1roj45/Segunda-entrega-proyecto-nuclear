package co.edu.humboldt.plataformacasos.compartido.excepcion;

/** El usuario está autenticado pero no tiene el permiso requerido: se responde 403. */
public class AccesoDenegadoException extends RuntimeException {

    public static final String MENSAJE = "No tiene permiso para realizar esta operación.";

    public AccesoDenegadoException() {
        super(MENSAJE);
    }

    /** Con un motivo concreto (por ejemplo, el recurso pertenece a otro usuario). */
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
