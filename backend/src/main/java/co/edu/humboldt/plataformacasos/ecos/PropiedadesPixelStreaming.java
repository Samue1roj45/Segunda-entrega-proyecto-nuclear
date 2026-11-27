package co.edu.humboldt.plataformacasos.ecos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Pixel Streaming de «Ecos de una decisión» (variable ECOS_PIXEL_STREAMING_URL).
 *
 * @param url dirección pública del reproductor de Pixel Streaming (http o https). Vacía = el juego se abre en el
 *            computador del estudiante con el enlace ecos://, como hasta ahora.
 */
@ConfigurationProperties(prefix = "app.ecos.pixel-streaming")
public record PropiedadesPixelStreaming(@DefaultValue("") String url) {
}
