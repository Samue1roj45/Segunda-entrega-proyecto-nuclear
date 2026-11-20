package co.edu.humboldt.plataformacasos.autenticacion.servicio;

import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Protege el ingreso contra adivinar claves: tras {@code maximo} fallos seguidos con el mismo correo bloquea ese
 * correo durante {@code minutos} y avisa a los administradores. Un ingreso correcto reinicia la cuenta.
 * Usa su propia transacción para que los fallos queden guardados aunque la petición termine en error.
 */
@Service
public class ControlAcceso {

    private final JdbcTemplate jdbc;
    private final Clock reloj;
    private final ServicioAvisos avisos;
    private final UsuarioRepositorio usuarios;
    private final int maximo;
    private final long minutos;

    public ControlAcceso(JdbcTemplate jdbc, Clock reloj, ServicioAvisos avisos, UsuarioRepositorio usuarios,
                         @Value("${app.autenticacion.intentos-maximos:5}") int maximo,
                         @Value("${app.autenticacion.minutos-bloqueo:15}") long minutos) {
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.avisos = avisos;
        this.usuarios = usuarios;
        this.maximo = maximo;
        this.minutos = minutos;
    }

    /** Lanza {@link CuentaBloqueadaException} si el correo está bloqueado ahora mismo. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void exigirLibre(String correo) {
        List<Timestamp> hasta = jdbc.query("SELECT bloqueado_hasta FROM bloqueo_acceso WHERE correo = ?",
                (rs, n) -> rs.getTimestamp("bloqueado_hasta"), clave(correo));
        if (hasta.isEmpty() || hasta.get(0) == null) { return; }
        LocalDateTime fin = hasta.get(0).toLocalDateTime();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (fin.isAfter(ahora)) {
            throw new CuentaBloqueadaException(Math.max(1, Duration.between(ahora, fin).plusSeconds(59).toMinutes()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(String correo) {
        String c = clave(correo);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Object[]> filas = jdbc.query("SELECT intentos, bloqueado_hasta FROM bloqueo_acceso WHERE correo = ?",
                (rs, n) -> new Object[] { rs.getInt("intentos"), rs.getTimestamp("bloqueado_hasta") }, c);
        int intentos = 1;
        if (filas.isEmpty()) {
            jdbc.update("INSERT INTO bloqueo_acceso (correo, intentos, bloqueado_hasta, ultimo_intento) VALUES (?, 1, NULL, ?)",
                    c, Timestamp.valueOf(ahora));
        } else {
            Timestamp previo = (Timestamp) filas.get(0)[1];
            boolean expirado = previo != null && !previo.toLocalDateTime().isAfter(ahora);
            intentos = expirado ? 1 : (Integer) filas.get(0)[0] + 1;
            jdbc.update("UPDATE bloqueo_acceso SET intentos = ?, bloqueado_hasta = NULL, ultimo_intento = ? WHERE correo = ?",
                    intentos, Timestamp.valueOf(ahora), c);
        }
        if (intentos >= maximo) {
            jdbc.update("UPDATE bloqueo_acceso SET bloqueado_hasta = ? WHERE correo = ?",
                    Timestamp.valueOf(ahora.plusMinutes(minutos)), c);
            alertarBloqueo(c);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(String correo) {
        jdbc.update("DELETE FROM bloqueo_acceso WHERE correo = ?", clave(correo));
    }

    private void alertarBloqueo(String correo) {
        Optional<Usuario> usuario = usuarios.findByCorreoInstitucional(correo);
        String quien = usuario.map(u -> u.getNombreCompleto() + " (" + correo + ")").orElse(correo);
        String detalle = "Cuenta bloqueada " + minutos + " min tras " + maximo + " intentos fallidos: " + quien;
        avisos.registrar("CUENTA_BLOQUEADA", null, correo, detalle);
        avisos.avisarAdministradores("SEGURIDAD", "Cuenta bloqueada", detalle + ".");
        usuario.filter(u -> !u.esPersonal()).ifPresent(u -> avisos.avisar(avisos.docentesDelEstudiante(u.getId()),
                "SEGURIDAD", "Un estudiante fue bloqueado", u.getNombreCompleto() + " superó los intentos de ingreso y quedó bloqueado " + minutos + " minutos."));
    }

    private static String clave(String correo) {
        return correo == null ? "" : Usuario.normalizarCorreo(correo);
    }
}
