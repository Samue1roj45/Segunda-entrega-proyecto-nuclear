package co.edu.humboldt.plataformacasos.ecos;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Token de la sesión de juego. Se firma con una clave DERIVADA de la del sistema (no la misma), así un token del
 * juego nunca sirve como token web ni al revés. Solo vale para las rutas {sesionId}/eventos y {sesionId}/resultado.
 */
@Component
public class TokenEcos {

    private static final String EMISOR = "plataforma-casos-ecos";
    private static final String RECLAMO_ESTUDIANTE = "est";
    private static final String RECLAMO_TIPO = "tipo";
    private static final String TIPO = "ecos";

    public record Reclamos(String sesionId, String estudianteId) {
    }

    private final JwtEncoder codificador;
    private final JwtDecoder decodificador;
    private final PropiedadesEcos propiedades;
    private final Clock reloj;

    public TokenEcos(SecretKey claveFirmaJwt, PropiedadesEcos propiedades, Clock reloj) {
        SecretKey derivada = derivar(claveFirmaJwt);
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(derivada));
        this.decodificador = NimbusJwtDecoder.withSecretKey(derivada).macAlgorithm(MacAlgorithm.HS256).build();
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    private static SecretKey derivar(SecretKey base) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(base.getEncoded(), "HmacSHA256"));
            return new SecretKeySpec(mac.doFinal("ecos-soma/1".getBytes(StandardCharsets.UTF_8)), "HmacSHA256");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo derivar la clave del token de Ecos", e);
        }
    }

    public String emitir(String sesionId, String estudianteId) {
        Instant ahora = Instant.now(reloj);
        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(sesionId)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.horasToken(), ChronoUnit.HOURS))
                .claim(RECLAMO_ESTUDIANTE, estudianteId)
                .claim(RECLAMO_TIPO, TIPO)
                .build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos))
                .getTokenValue();
    }

    /** Lee el encabezado Authorization: Bearer ...; cualquier problema es 401 TOKEN_INVALIDO. */
    public Reclamos verificar(String encabezado) {
        if (encabezado == null || !encabezado.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw invalido();
        }
        try {
            Jwt jwt = decodificador.decode(encabezado.substring(7).trim());
            if (!EMISOR.equals(jwt.getClaimAsString("iss"))
                    || !TIPO.equals(jwt.getClaimAsString(RECLAMO_TIPO))) {
                throw invalido();
            }
            return new Reclamos(jwt.getSubject(), jwt.getClaimAsString(RECLAMO_ESTUDIANTE));
        } catch (JwtException e) {
            throw invalido();
        }
    }

    private static EcosException invalido() {
        return new EcosException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "El token de la sesión de juego no es válido o venció.");
    }
}
