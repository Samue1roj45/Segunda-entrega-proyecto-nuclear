package co.edu.humboldt.plataformacasos.compartido.excepcion;

/** No hay una sesión válida (o el usuario ya no está activo): se responde 401. */
public class NoAutenticadoException extends RuntimeException {

    public static final String MENSAJE = "Debe iniciar sesión para acceder a este recurso.";

    public NoAutenticadoException() {
        super(MENSAJE);
    }
}
