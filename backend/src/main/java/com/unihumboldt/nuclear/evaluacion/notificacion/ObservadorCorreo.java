package com.unihumboldt.nuclear.evaluacion.notificacion;

import org.springframework.stereotype.Component;

@Component
public class ObservadorCorreo implements ObservadorAcademico {

    private final NotificacionCorreoServicio correo;

    public ObservadorCorreo(NotificacionCorreoServicio correo) {
        this.correo = correo;
    }

    @Override
    public ResultadoEnvio recibir(EventoAcademico evento) {
        return correo.emitir(evento);
    }
}
