package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import org.springframework.stereotype.Component;

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
