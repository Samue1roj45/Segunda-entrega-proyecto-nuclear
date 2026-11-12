package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;

/** Transición de estado no permitida para un intento (State, RF-12): se responde 400 como regla de negocio. */
public class TransicionInvalidaException extends ReglaNegocioException {

    public TransicionInvalidaException(String message) {
        super(message);
    }
}
