package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import org.springframework.stereotype.Component;

// Patrón Observer: observador concreto que envía el correo de cada evento académico.
@Component
public class ObservadorCorreo implements ObservadorAcademico {

    private final ServicioCorreoNotificaciones correo;

    public ObservadorCorreo(ServicioCorreoNotificaciones correo) {
        this.correo = correo;
    }

    @Override
    public ResultadoEnvio recibir(EventoAcademico evento) {
        return correo.emitir(evento);
    }
}
