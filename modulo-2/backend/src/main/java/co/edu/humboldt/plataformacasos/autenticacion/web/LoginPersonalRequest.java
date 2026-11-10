package co.edu.humboldt.plataformacasos.autenticacion.web;

/** Cuerpo de POST /api/auth/personal/login (docentes y administrador). */
public record LoginPersonalRequest(String correoInstitucional, String clave) {
}
