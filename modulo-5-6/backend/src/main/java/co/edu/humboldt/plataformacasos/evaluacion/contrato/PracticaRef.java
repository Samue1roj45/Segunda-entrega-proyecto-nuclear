package co.edu.humboldt.plataformacasos.evaluacion.contrato;

import java.time.OffsetDateTime;

/**
 * Sesión (práctica) programada en el módulo 3. Estados: SIN_INICIAR, EN_CURSO y FINALIZADO (RF-19).
 * nombre es el nombre del caso; tiempoMaximoMinutos es el tiempo de cada intento (RF-23).
 */
public record PracticaRef(
        Long id,
        String nombre,
        Long casoId,
        Long docenteId,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        String estado,
        String mensajePersonalizado,
        int tiempoMaximoMinutos) {

    public static final String SIN_INICIAR = "SIN_INICIAR";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADO = "FINALIZADO";

    public boolean finalizada() {
        return FINALIZADO.equals(estado);
    }
}
