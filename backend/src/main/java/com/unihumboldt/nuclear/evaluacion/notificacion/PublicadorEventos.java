package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PublicadorEventos {

    private final List<ObservadorAcademico> observadores;

    public PublicadorEventos(List<ObservadorAcademico> observadores) {
        this.observadores = observadores;
    }

    public ResultadoEnvio publicar(EventoAcademico evento) {
        ResultadoEnvio ultimo = ResultadoEnvio.OMITIDA_DUPLICADO;
        for (ObservadorAcademico observador : observadores) {
            ultimo = observador.recibir(evento);
        }
        return ultimo;
    }
}
