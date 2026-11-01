package co.edu.humboldt.plataformacasos.compartido.validacion;

import java.util.Objects;

/**
 * Implementación base de la cadena. Cada subclase solo implementa su condición
 * en {@link #validarCondicion(Object)}; el recorrido de la cadena queda aquí
 * (método plantilla), así no se repite en cada validador.
 */
public abstract class ValidadorBase<T> implements Validador<T> {

    private Validador<T> siguiente;

    @Override
    public final Validador<T> establecerSiguiente(Validador<T> siguiente) {
        this.siguiente = Objects.requireNonNull(siguiente, "siguiente");
        return siguiente;
    }

    @Override
    public final ResultadoValidacion validar(T solicitud) {
        ResultadoValidacion resultado = validarCondicion(solicitud);
        if (!resultado.aprobado() || siguiente == null) {
            return resultado;
        }
        return siguiente.validar(solicitud);
    }

    /** Comprueba una única condición. No debe llamar al siguiente eslabón. */
    protected abstract ResultadoValidacion validarCondicion(T solicitud);
}
