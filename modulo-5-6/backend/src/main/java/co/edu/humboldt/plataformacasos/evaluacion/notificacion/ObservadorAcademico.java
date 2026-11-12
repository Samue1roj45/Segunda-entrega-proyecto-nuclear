package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

public interface ObservadorAcademico {

    ResultadoEnvio recibir(EventoAcademico evento);
}
