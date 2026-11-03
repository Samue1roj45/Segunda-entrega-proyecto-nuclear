package co.edu.humboldt.plataformacasos.autenticacion.token;

import co.edu.humboldt.plataformacasos.configuracion.seguridad.PropiedadesSeguridad;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Emite el token de sesión firmado con el rol y los permisos efectivos del usuario. */
@Component
public class EmisorToken {

    public static final String EMISOR = "plataforma-casos";
    public static final String RECLAMO_CORREO = "correo";
    public static final String RECLAMO_ROL = "rol";
    public static final String RECLAMO_PERMISOS = "permisos";
    private static final String TIPO_TOKEN = "Bearer";

    private final JwtEncoder codificador;
    private final PropiedadesSeguridad propiedades;
    private final Clock reloj;

    @Autowired
    public EmisorToken(JwtEncoder codificador, PropiedadesSeguridad propiedades) {
        this(codificador, propiedades, Clock.systemUTC());
    }

    EmisorToken(JwtEncoder codificador, PropiedadesSeguridad propiedades, Clock reloj) {
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public SesionUsuario emitir(Usuario usuario) {
        Instant ahora = Instant.now(reloj);
        Instant expira = ahora.plus(propiedades.minutosToken(), ChronoUnit.MINUTES);
        Set<String> permisos = usuario.permisosEfectivos().stream()
                .map(Permiso::name)
                .collect(Collectors.toCollection(TreeSet::new));

        JwtClaimsSet reclamos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(expira)
                .claim(RECLAMO_CORREO, usuario.getCorreoInstitucional())
                .claim(RECLAMO_ROL, usuario.getRol().name())
                .claim(RECLAMO_PERMISOS, permisos)
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificador.encode(JwtEncoderParameters.from(cabecera, reclamos)).getTokenValue();

        return new SesionUsuario(token, TIPO_TOKEN, expira, usuario.getCorreoInstitucional(),
                usuario.getNombreCompleto(), usuario.getRol().name(), permisos);
    }
}
