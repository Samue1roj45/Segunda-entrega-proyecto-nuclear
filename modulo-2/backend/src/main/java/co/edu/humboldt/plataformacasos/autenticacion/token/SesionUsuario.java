package co.edu.humboldt.plataformacasos.autenticacion.token;

import java.time.Instant;
import java.util.Set;

/** Respuesta de un inicio de sesión exitoso. El frontend usa rol y permisos solo para mostrar opciones. */
public record SesionUsuario(
        String token,
        String tipoToken,
        Instant expiraEn,
        String correoInstitucional,
        String nombreCompleto,
        String rol,
        Set<String> permisos) {
}
