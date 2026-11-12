package com.unihumboldt.nuclear.evaluacion.notificacion;

public interface ObservadorAcademico {

    ResultadoEnvio recibir(EventoAcademico evento);
}
