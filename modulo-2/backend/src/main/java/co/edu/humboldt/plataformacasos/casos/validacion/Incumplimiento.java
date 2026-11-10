package co.edu.humboldt.plataformacasos.casos.validacion;

/** Una regla OCL que el caso no cumple y dónde corregirla. */
public record Incumplimiento(String regla, String mensaje, SeccionCaso seccion, String ubicacion) {
}
