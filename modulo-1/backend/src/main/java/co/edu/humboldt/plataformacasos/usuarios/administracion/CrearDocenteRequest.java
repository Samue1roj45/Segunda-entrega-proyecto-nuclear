package co.edu.humboldt.plataformacasos.usuarios.administracion;

/** Cuerpo de POST /api/admin/docentes. */
public record CrearDocenteRequest(String nombreCompleto, String correoInstitucional) {
}
