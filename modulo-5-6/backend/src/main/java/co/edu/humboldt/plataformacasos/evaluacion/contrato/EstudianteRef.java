package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Estudiante (usuario con rol ESTUDIANTE del módulo 1). id y usuarioId coinciden en la base única.
 */
public record EstudianteRef(Long id, String correo, String nombreCompleto, Long usuarioId) {
}
