package co.edu.humboldt.plataformacasos.configuracion.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parámetros de seguridad leídos de application.yml / variables de entorno.
 *
 * @param jwtSecreto   clave HMAC en Base64 (mínimo 32 bytes). Vacía = clave aleatoria al arrancar.
 * @param minutosToken vigencia del token de sesión en minutos.
 */
@ConfigurationProperties(prefix = "app.seguridad")
public record PropiedadesSeguridad(String jwtSecreto, @DefaultValue("60") long minutosToken) {

    public PropiedadesSeguridad {
        if (minutosToken <= 0) {
            throw new IllegalArgumentException("app.seguridad.minutos-token debe ser mayor que cero");
        }
    }
}
