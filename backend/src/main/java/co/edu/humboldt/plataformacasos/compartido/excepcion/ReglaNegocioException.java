package co.edu.humboldt.plataformacasos.compartido.excepcion;

/**
 * La operación incumple una regla de negocio (sesión no iniciada, intento cerrado, ingreso tardío...):
 * se responde 400 con el mensaje. Es la excepción común de los módulos 3 a 6.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
