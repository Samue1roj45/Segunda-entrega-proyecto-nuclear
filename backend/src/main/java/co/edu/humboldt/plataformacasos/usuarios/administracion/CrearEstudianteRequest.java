package co.edu.humboldt.plataformacasos.usuarios.administracion;

/** Cuerpo de POST /api/admin/estudiantes. */
public record CrearEstudianteRequest(String nombreCompleto, String correoInstitucional) {
}
