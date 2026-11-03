package co.edu.humboldt.plataformacasos.compartido.validacion;

import java.util.Objects;

/**
 * Resultado de un eslabón de la cadena de validación (Chain of Responsibility).
 * Clase compartida entre módulos: unifica los campos de los diagramas 6.1 y 6.2
 * del documento de patrones (aprobado / mensaje).
 */
public record ResultadoValidacion(boolean aprobado, String mensaje) {

    private static final ResultadoValidacion APROBADO = new ResultadoValidacion(true, "");

    public ResultadoValidacion {
        Objects.requireNonNull(mensaje, "mensaje");
    }

    public static ResultadoValidacion exito() {
        return APROBADO;
    }

    public static ResultadoValidacion rechazo(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("Un rechazo debe explicar el motivo");
        }
        return new ResultadoValidacion(false, mensaje);
    }
}
