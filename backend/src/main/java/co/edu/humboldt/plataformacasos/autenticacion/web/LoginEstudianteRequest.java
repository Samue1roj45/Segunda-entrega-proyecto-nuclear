package co.edu.humboldt.plataformacasos.autenticacion.web;

/** Cuerpo de POST /api/auth/estudiantes/login. La validación la hace la cadena de RF-01. */
public record LoginEstudianteRequest(String correoInstitucional, String codigoAutorizacion) {
}
