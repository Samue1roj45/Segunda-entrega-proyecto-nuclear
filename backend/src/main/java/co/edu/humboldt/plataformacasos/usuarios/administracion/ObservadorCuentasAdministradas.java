package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

// Patrón Observer: reacciona a los cambios de cuentas, después de confirmarlos, con auditoría y avisos.
/**
 * Deja la constancia en la auditoría y avisa a los administradores solo cuando el cambio ya se confirmó.
 * Hacerlo dentro de la transacción del cambio es inseguro: esa transacción tiene bloqueadas las filas de
 * {@code usuario}, y los avisos (con llave hacia {@code usuario}) se escriben en una transacción propia
 * que quedaría esperando ese bloqueo. Así, además, la auditoría solo registra lo que realmente ocurrió.
 */
@Component
public class ObservadorCuentasAdministradas {

    private final ServicioAvisos avisos;

    public ObservadorCuentasAdministradas(ServicioAvisos avisos) {
        this.avisos = avisos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alAdministrarCuenta(CuentaAdministradaEvento evento) {
        avisos.registrar(evento.accion(), evento.actorId(), evento.actorCorreo(), evento.detalle());
        avisos.avisarAdministradores("ADMINISTRACION", "Cambio en cuentas", evento.detalle() + ".");
        if (evento.idCuentaAvisada() != null) {
            avisos.avisar(List.of(evento.idCuentaAvisada()), "CUENTA", evento.tituloAviso(), evento.mensajeAviso());
        }
    }
}
