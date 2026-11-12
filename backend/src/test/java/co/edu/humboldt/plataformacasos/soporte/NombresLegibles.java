package co.edu.humboldt.plataformacasos.soporte;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * Convierte los nombres de las pruebas en frases legibles para el reporte de consola:
 * {@code noFinalizaDesdeNoIniciada} → "No finaliza desde no iniciada". Las pruebas con
 * {@code @DisplayName} conservan su nombre. Se activa en junit-platform.properties.
 */
public class NombresLegibles extends DisplayNameGenerator.Standard {

    @Override
    public String generateDisplayNameForClass(Class<?> clase) {
        return frase(clase.getSimpleName().replaceAll("Test$", ""));
    }

    @Override
    public String generateDisplayNameForNestedClass(List<Class<?>> envolventes, Class<?> clase) {
        return frase(clase.getSimpleName());
    }

    @Override
    public String generateDisplayNameForMethod(List<Class<?>> envolventes, Class<?> clase, Method metodo) {
        return frase(metodo.getName());
    }

    static String frase(String nombre) {
        String separado = nombre.replace('_', ' ')
                .replaceAll("([a-zá-ú0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-zA-Z])(\\d)", "$1 $2")
                .toLowerCase();
        return separado.isEmpty() ? nombre : Character.toUpperCase(separado.charAt(0)) + separado.substring(1);
    }
}
