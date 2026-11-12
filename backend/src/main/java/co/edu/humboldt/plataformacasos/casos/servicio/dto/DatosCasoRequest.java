package co.edu.humboldt.plataformacasos.casos.servicio.dto;

/** Datos académicos del caso (RF-05). Pueden venir incompletos: el caso sigue siendo un borrador. */
public record DatosCasoRequest(String nombre, String desarrolloSituacional, String contextoHistoria,
                               Integer tiempoEstimadoMinutos) {
}
