package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Estudiante autorizado en una sesión (módulo 3, RF-04). intentosPermitidos = 1 + repeticiones
 * excepcionales concedidas por el docente creador del caso (RF-25, como máximo dos intentos).
 */
public record AutorizacionRef(
        Long id,
        Long practicaId,
        Long estudianteId,
        boolean activa,
        int intentosPermitidos) {
}
