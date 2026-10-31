package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PublicadorEventos {

    private final List<ObservadorAcademico> observadores;

    public PublicadorEventos(List<ObservadorAcademico> observadores) {
        this.observadores = List.copyOf(observadores);
    }

    /**
     * Notifica a todos los observadores y acumula sus envíos.
     * Un observador no sustituye el resultado de los demás.
     */
    public ResumenEnvio publicar(EventoAcademico evento) {
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        for (ObservadorAcademico observador : observadores) {
            resumen = resumen.acumular(observador.recibir(evento));
        }
        return resumen;
    }
}
