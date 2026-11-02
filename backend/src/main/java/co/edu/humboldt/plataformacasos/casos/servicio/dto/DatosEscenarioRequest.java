package co.edu.humboldt.plataformacasos.casos.servicio.dto;

/** Datos de un escenario (RF-06). Pueden venir incompletos mientras el caso sea un borrador. */
public record DatosEscenarioRequest(String nombre, String contexto) {
}
