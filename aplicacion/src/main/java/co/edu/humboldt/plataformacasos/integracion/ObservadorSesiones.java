package co.edu.humboldt.plataformacasos.integracion;

import co.edu.humboldt.plataformacasos.evaluacion.notificacion.FachadaNotificaciones;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.CierrePendientes;
import co.edu.humboldt.plataformacasos.sesiones.evento.EstudianteConvocadoEvento;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionFinalizadaEvento;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionModificadaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

// Patrón Observer: escucha los eventos de M3 (convocatoria, modificación, finalización) y dispara notificaciones y cierres en M5.
/**
 * Observer entre módulos: los eventos del módulo 3 disparan la evaluación y las notificaciones de los
 * módulos 5 y 6.
 * <ul>
 *   <li>SesionFinalizadaEvento (RF-19) → RF-24: cierra intentos en curso como INCOMPLETOS, registra
 *       ausencias y califica. Misma transacción: si falla, la sesión no queda finalizada.</li>
 *   <li>EstudianteConvocadoEvento (RF-04, RF-20) → RF-21: correo de convocatoria con el código de acceso.</li>
 *   <li>SesionModificadaEvento (RF-18) → RF-21: aviso de cambio a los convocados.</li>
 * </ul>
 * Los correos se envían después de confirmar la transacción, en una transacción propia: un fallo SMTP no
 * deshace la programación (queda registrado como notificación con ERROR).
 */
@Component
public class ObservadorSesiones {

    private static final Logger LOG = LoggerFactory.getLogger(ObservadorSesiones.class);
    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CierrePendientes cierre;
    private final FachadaNotificaciones notificaciones;

    public ObservadorSesiones(CierrePendientes cierre, FachadaNotificaciones notificaciones) {
        this.cierre = cierre;
        this.notificaciones = notificaciones;
    }

    @EventListener
    public void alFinalizarSesion(SesionFinalizadaEvento evento) {
        var resumen = cierre.evaluarAlFinalizarSesion(evento.idPractica());
        LOG.info("Sesión {} finalizada ({}): {} incompletas, {} ausencias", evento.idPractica(),
                evento.tipoCierre(), resumen.incompletas(), resumen.ausencias());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alConvocarEstudiante(EstudianteConvocadoEvento evento) {
        notificaciones.convocarEstudiante(evento.idPractica(), evento.idEstudiante(), evento.codigoAcceso());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alModificarSesion(SesionModificadaEvento evento) {
        notificaciones.notificarCambio(evento.idPractica(), evento.motivo(), evento.fecha().format(MARCA));
    }
}
