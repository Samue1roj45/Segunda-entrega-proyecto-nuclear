package co.edu.humboldt.plataformacasos.autenticacion.servicio;

/** Demasiados intentos fallidos: se responde 429 hasta que pase el tiempo de bloqueo. */
public class CuentaBloqueadaException extends RuntimeException {

    public CuentaBloqueadaException(long minutos) {
        super("Demasiados intentos fallidos. Intenta de nuevo en " + minutos + (minutos == 1 ? " minuto." : " minutos."));
    }
}
