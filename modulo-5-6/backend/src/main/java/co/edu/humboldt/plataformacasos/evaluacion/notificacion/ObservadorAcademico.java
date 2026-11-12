package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

// Patrón Observer: contrato de los observadores que reaccionan a eventos académicos.
public interface ObservadorAcademico {

    ResultadoEnvio recibir(EventoAcademico evento);
}
