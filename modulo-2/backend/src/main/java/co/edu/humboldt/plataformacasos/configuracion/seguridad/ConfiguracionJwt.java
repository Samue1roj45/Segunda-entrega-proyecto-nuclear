package co.edu.humboldt.plataformacasos.configuracion.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Firma y verificación de los tokens de sesión (HS256). Se usa la implementación
 * de Spring Security (Nimbus) en lugar de criptografía hecha a mano.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class ConfiguracionJwt {

    private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionJwt.class);
    private static final String ALGORITMO_HMAC = "HmacSHA256";
    private static final int BYTES_MINIMOS_CLAVE = 32;

    @Bean
    SecretKey claveFirmaJwt(PropiedadesSeguridad propiedades) {
        String secreto = propiedades.jwtSecreto();
        if (secreto == null || secreto.isBlank()) {
            LOG.warn("JWT_SECRETO no está definido: se usa una clave aleatoria. "
                    + "Los tokens dejarán de ser válidos al reiniciar la aplicación.");
            byte[] aleatoria = new byte[BYTES_MINIMOS_CLAVE];
            new SecureRandom().nextBytes(aleatoria);
            return new SecretKeySpec(aleatoria, ALGORITMO_HMAC);
        }
        byte[] clave = Base64.getDecoder().decode(secreto.trim());
        if (clave.length < BYTES_MINIMOS_CLAVE) {
            throw new IllegalStateException("JWT_SECRETO debe tener al menos " + BYTES_MINIMOS_CLAVE + " bytes");
        }
        return new SecretKeySpec(clave, ALGORITMO_HMAC);
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey claveFirmaJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveFirmaJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey claveFirmaJwt) {
        return NimbusJwtDecoder.withSecretKey(claveFirmaJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
