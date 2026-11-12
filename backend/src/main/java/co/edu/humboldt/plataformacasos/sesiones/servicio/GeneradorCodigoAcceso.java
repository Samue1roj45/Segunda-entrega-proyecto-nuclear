package co.edu.humboldt.plataformacasos.sesiones.servicio;

import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/*
 #REQUISITO RF-20
 Para qué sirve: genera el código de acceso de cada estudiante convocado. Es aleatorio (SecureRandom),
 en mayúsculas y sin caracteres que se confunden al leerlos (0/O, 1/I/L). Solo se guarda su hash.
 */
@Component
public class GeneradorCodigoAcceso {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_MINIMA = 6;

    private final SecureRandom aleatorio = new SecureRandom();
    private final int longitud;

    public GeneradorCodigoAcceso(@Value("${app.sesiones.longitud-codigo:8}") int longitud) {
        if (longitud < LONGITUD_MINIMA) {
            throw new IllegalArgumentException("app.sesiones.longitud-codigo debe ser al menos " + LONGITUD_MINIMA);
        }
        this.longitud = longitud;
    }

    public String generar() {
        StringBuilder codigo = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
