package co.edu.humboldt.plataformacasos.avisos;

import co.edu.humboldt.plataformacasos.autorizacion.UsuarioAutenticado;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Limpieza controlada del historial de auditoría por fecha de corte. Borra solo los registros con fecha
 * anterior al inicio del día elegido y, en la misma transacción, deja un registro {@code AUDITORIA_LIMPIADA}
 * con el responsable, el corte y la cantidad borrada. Ese registro lleva la fecha actual, que nunca es
 * anterior al corte, así que la limpieza no lo puede borrar. La auditoría sigue registrando con normalidad.
 */
@Service
public class LimpiezaAuditoria {

    public static final String ACCION_LIMPIEZA = "AUDITORIA_LIMPIADA";
    public static final String ACCION_FALLO = "AUDITORIA_LIMPIEZA_FALLIDA";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

    public record VistaPrevia(LocalDate antesDe, long registros) {
    }

    public record Resultado(LocalDate antesDe, long eliminados, LocalDateTime fecha) {
    }

    private final JdbcTemplate jdbc;
    private final Clock reloj;
    private final ServicioAvisos avisos;
    private final TransactionTemplate transaccion;

    public LimpiezaAuditoria(JdbcTemplate jdbc, Clock reloj, ServicioAvisos avisos, PlatformTransactionManager transacciones) {
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.avisos = avisos;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    /** Cuántos registros borraría la limpieza con este corte. No cambia nada. */
    @Transactional(readOnly = true)
    public VistaPrevia previsualizar(LocalDate antesDe) {
        Timestamp corte = corteValido(antesDe);
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE fecha < ?", Long.class, corte);
        return new VistaPrevia(antesDe, n == null ? 0 : n);
    }

    /**
     * Borra y registra en una sola transacción: o queda todo (borrado y constancia) o nada. Si falla, el
     * fallo se registra después del rollback, cuando ya no hay bloqueos sobre la tabla, para que no se pierda.
     */
    public Resultado ejecutar(LocalDate antesDe, UsuarioAutenticado actor) {
        Timestamp corte = corteValido(antesDe);
        try {
            return transaccion.execute(estado -> {
                int eliminados = jdbc.update("DELETE FROM auditoria WHERE fecha < ?", corte);
                String cantidad = eliminados == 1 ? "1 registro" : eliminados + " registros";
                avisos.registrarEnLaMismaTransaccion(ACCION_LIMPIEZA, actor.id(), actor.correoInstitucional(),
                        actor.nombreCompleto() + " limpió el historial de auditoría: se eliminaron " + cantidad
                                + " anteriores al " + FORMATO.format(antesDe) + ". Resultado: correcto");
                return new Resultado(antesDe, eliminados, LocalDateTime.now(reloj));
            });
        } catch (RuntimeException e) {
            avisos.registrar(ACCION_FALLO, actor.id(), actor.correoInstitucional(),
                    actor.nombreCompleto() + " intentó limpiar el historial de auditoría (corte: " + FORMATO.format(antesDe)
                            + "). Resultado: falló y no se eliminó ningún registro");
            throw e;
        }
    }

    /** La fecha es obligatoria y no puede ser posterior a hoy. El corte es el inicio de ese día. */
    private Timestamp corteValido(LocalDate antesDe) {
        if (antesDe == null) {
            throw new SolicitudInvalidaException("Selecciona la fecha de corte.");
        }
        if (antesDe.isAfter(LocalDate.now(reloj))) {
            throw new SolicitudInvalidaException("La fecha de corte no puede ser posterior a hoy.");
        }
        return Timestamp.valueOf(antesDe.atStartOfDay());
    }
}
