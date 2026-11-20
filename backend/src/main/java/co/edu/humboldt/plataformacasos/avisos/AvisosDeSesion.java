package co.edu.humboldt.plataformacasos.avisos;

import co.edu.humboldt.plataformacasos.sesiones.evento.SesionFinalizadaEvento;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionIniciadaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/** Avisos cuando una sesión empieza o termina: a los estudiantes convocados y al docente dueño. Solo tras confirmar la transacción. */
@Component
class AvisosDeSesion {

    private static final Logger LOG = LoggerFactory.getLogger(AvisosDeSesion.class);

    private final ServicioAvisos avisos;
    private final JdbcTemplate jdbc;

    AvisosDeSesion(ServicioAvisos avisos, JdbcTemplate jdbc) {
        this.avisos = avisos;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alIniciar(SesionIniciadaEvento e) {
        try {
            avisos.avisar(convocados(e.idPractica()), "SESION_INICIADA", "Una sesión comenzó",
                    "La sesión «" + nombre(e.idPractica()) + "» ya está en curso. Ya puedes participar.");
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo avisar el inicio de la sesión {}: {}", e.idPractica(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alFinalizar(SesionFinalizadaEvento e) {
        try {
            String nombre = nombre(e.idPractica());
            boolean auto = "AUTOMATICO".equals(e.tipoCierre());
            avisos.avisar(convocados(e.idPractica()), "SESION_FINALIZADA", "Una sesión terminó",
                    "La sesión «" + nombre + "» terminó" + (auto ? " porque venció su plazo." : "."));
            avisos.avisar(jdbc.queryForList("SELECT id_docente FROM practicas WHERE id_practica = ?", Long.class, e.idPractica()),
                    "SESION_FINALIZADA", "Sesión finalizada",
                    "La sesión «" + nombre + "» se finalizó" + (auto ? " automáticamente por vencimiento del plazo." : ".") + " Ya puedes revisar los resultados.");
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo avisar el cierre de la sesión {}: {}", e.idPractica(), ex.getMessage());
        }
    }

    private List<Long> convocados(Long idPractica) {
        return jdbc.queryForList("SELECT id_estudiante FROM autorizaciones WHERE id_practica = ? AND estado = 'ACTIVA'", Long.class, idPractica);
    }

    private String nombre(Long idPractica) {
        List<String> n = jdbc.queryForList("SELECT c.nombre FROM practicas p JOIN casos_estudio c ON c.id_caso = p.id_caso WHERE p.id_practica = ?", String.class, idPractica);
        return n.isEmpty() || n.get(0) == null ? "(sin nombre)" : n.get(0);
    }
}
