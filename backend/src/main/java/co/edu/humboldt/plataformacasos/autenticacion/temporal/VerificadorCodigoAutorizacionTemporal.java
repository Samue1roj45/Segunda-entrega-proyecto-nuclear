package co.edu.humboldt.plataformacasos.autenticacion.temporal;

import co.edu.humboldt.plataformacasos.autenticacion.puerto.VerificadorCodigoAutorizacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TEMPORAL: verifica el código contra la tabla codigo_autorizacion_temporal mientras el
 * Módulo 3 (RF-20) publica su implementación de {@link VerificadorCodigoAutorizacion}.
 * Cuando exista, se borra esta clase y la migración V2; el resto del Módulo 1 no cambia.
 */
@Component
public class VerificadorCodigoAutorizacionTemporal implements VerificadorCodigoAutorizacion {

    private static final String CONSULTA_HASH =
            "SELECT codigo_hash FROM codigo_autorizacion_temporal WHERE estudiante_id = ?";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder codificador;

    public VerificadorCodigoAutorizacionTemporal(JdbcTemplate jdbc, PasswordEncoder codificador) {
        this.jdbc = jdbc;
        this.codificador = codificador;
    }

    @Override
    public boolean esValido(Long estudianteId, String codigoAutorizacion) {
        List<String> hashes = jdbc.queryForList(CONSULTA_HASH, String.class, estudianteId);
        return hashes.stream().anyMatch(hash -> codificador.matches(codigoAutorizacion, hash));
    }
}
