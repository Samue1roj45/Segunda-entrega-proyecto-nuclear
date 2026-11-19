package co.edu.humboldt.plataformacasos.usuarios.web;

/** Estudiante tal como lo ve el docente al convocar: identificador, nombre y correo institucional. */
public record EstudianteResumenDto(Long id, String nombreCompleto, String correoInstitucional) {
}
