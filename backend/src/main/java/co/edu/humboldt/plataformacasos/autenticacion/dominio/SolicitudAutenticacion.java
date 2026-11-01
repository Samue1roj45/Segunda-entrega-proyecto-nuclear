package co.edu.humboldt.plataformacasos.autenticacion.dominio;

/** Datos con los que un estudiante pide ingresar (RF-01). Pueden venir vacíos o mal formados. */
public record SolicitudAutenticacion(String correoInstitucional, String codigoAutorizacion) {
}
