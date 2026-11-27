package co.edu.humboldt.plataformacasos.autenticacion.google;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Verifica firma (claves públicas de Google), vigencia, emisor y que el token se emitió para ESTA aplicación. */
@Component
class VerificadorGoogleJwks implements VerificadorGoogle {

    private static final Logger LOG = LoggerFactory.getLogger(VerificadorGoogleJwks.class);
    private static final String JWKS = "https://www.googleapis.com/oauth2/v3/certs";

    private final PropiedadesGoogle propiedades;
    private volatile JwtDecoder decodificador;

    VerificadorGoogleJwks(PropiedadesGoogle propiedades) { this.propiedades = propiedades; }

    @Override
    public Optional<IdentidadGoogle> verificar(String credencial) {
        try {
            Jwt jwt = decodificador().decode(credencial);
            String correo = jwt.getClaimAsString("email");
            if (correo == null) { return Optional.empty(); }
            return Optional.of(new IdentidadGoogle(correo, Boolean.TRUE.equals(jwt.getClaim("email_verified"))));
        } catch (RuntimeException e) {
            LOG.info("Token de Google rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private JwtDecoder decodificador() {
        if (decodificador == null) {
            NimbusJwtDecoder d = NimbusJwtDecoder.withJwkSetUri(JWKS).build();
            OAuth2TokenValidator<Jwt> emisor = jwt -> {
                String iss = jwt.getClaimAsString("iss");
                return "https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emisor no es Google", null));
            };
            OAuth2TokenValidator<Jwt> audiencia = jwt -> {
                List<String> aud = jwt.getAudience();
                return aud != null && aud.contains(propiedades.clientId())
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia incorrecta", null));
            };
            d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), emisor, audiencia));
            decodificador = d;
        }
        return decodificador;
    }
}
