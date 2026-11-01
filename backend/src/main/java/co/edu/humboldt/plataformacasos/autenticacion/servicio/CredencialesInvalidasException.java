package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;

/** Credenciales rechazadas: se responde 401 con un mensaje genérico. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        this(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
    }

    public CredencialesInvalidasException(String mensajeGenerico) {
        super(mensajeGenerico);
    }
}
