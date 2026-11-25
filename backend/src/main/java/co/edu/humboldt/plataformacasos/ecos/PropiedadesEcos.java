package co.edu.humboldt.plataformacasos.ecos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de la integración con el juego (application.yml / variables de entorno).
 *
 * @param apiPublica        URL pública de esta API tal como la debe usar el juego (va en el enlace ecos://)
 * @param minutosCodigo     vigencia del código de sesión si no se canjea
 * @param horasToken        vigencia del token que recibe el juego al canjear
 * @param maxIntentos       intentos permitidos por estudiante (decisión pendiente del equipo)
 * @param toleranciaSegundos holgura al comparar el tiempo informado con el tiempo real
 */
@ConfigurationProperties(prefix = "app.ecos")
public record PropiedadesEcos(
        @DefaultValue("http://localhost:8080/api/ecos/v1") String apiPublica,
        @DefaultValue("30") long minutosCodigo,
        @DefaultValue("6") long horasToken,
        @DefaultValue("1") int maxIntentos,
        @DefaultValue("10") long toleranciaSegundos) {
}
