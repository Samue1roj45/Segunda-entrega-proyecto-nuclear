package co.edu.humboldt.plataformacasos.compartido.web;

import java.time.LocalDateTime;

/** Cuerpo único de error de la API, usado por todos los manejadores (400, 401, 403, 409...). */
public record ErrorApi(LocalDateTime fecha, int estado, String error, String mensaje, String ruta) {

    public static ErrorApi de(int estado, String error, String mensaje, String ruta) {
        return new ErrorApi(LocalDateTime.now(), estado, error, mensaje, ruta);
    }
}
