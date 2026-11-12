package co.edu.humboldt.plataformacasos.soporte;

import java.util.Optional;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Reporte legible de cada prueba en la consola (mvn test o IntelliJ):
 *
 *   === [Validador dsl ceps] Ocl 04 preguntas
 *       → POST /api/casos {...}          (solo en pruebas de API, ver ReporteHttpPruebas)
 *       ← 400 {...}
 *       [OK] PASO  (12 ms)
 *
 * Se registra sola para todas las pruebas (autodetección en junit-platform.properties).
 */
public class ReporteConsola implements BeforeTestExecutionCallback, TestWatcher {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(ReporteConsola.class);

    @Override
    public void beforeTestExecution(ExtensionContext contexto) {
        String clase = contexto.getParent().map(ExtensionContext::getDisplayName).orElse("");
        System.out.println();
        System.out.println("=== [" + clase + "] " + contexto.getDisplayName());
        contexto.getStore(NS).put("inicio", System.currentTimeMillis());
    }

    @Override
    public void testSuccessful(ExtensionContext contexto) {
        System.out.println("    [OK] PASO" + duracion(contexto));
    }

    @Override
    public void testFailed(ExtensionContext contexto, Throwable causa) {
        System.out.println("    [X] FALLO" + duracion(contexto) + ": " + causa.getMessage());
    }

    @Override
    public void testDisabled(ExtensionContext contexto, Optional<String> razon) {
        System.out.println();
        System.out.println("=== " + contexto.getDisplayName() + "\n    [-] OMITIDA: " + razon.orElse("deshabilitada"));
    }

    private String duracion(ExtensionContext contexto) {
        Long inicio = contexto.getStore(NS).get("inicio", Long.class);
        return inicio == null ? "" : "  (" + (System.currentTimeMillis() - inicio) + " ms)";
    }
}
