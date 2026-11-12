package co.edu.humboldt.plataformacasos.sesiones.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Para qué sirve: una sesión (práctica) tal como está guardada: el caso que se programa, el docente que
 la programó, sus condiciones y su estado (SIN_INICIAR, EN_CURSO, FINALIZADO).
 */
public record Practica(Long id, Long idCaso, Long idDocente, String espacioAcademico,
                       LocalDateTime fechaInicio, LocalDateTime fechaFin, int tiempoMaximoMinutos,
                       BigDecimal porcentajeLimiteIngreso, String lugar, String mensajePersonalizado,
                       String urlAcceso, String estado, LocalDateTime fechaInicioReal,
                       LocalDateTime fechaCierreReal, String tipoCierre) {

    public static final String SIN_INICIAR = "SIN_INICIAR";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADO = "FINALIZADO";

    public boolean finalizada() {
        return FINALIZADO.equals(estado);
    }

    /** Instante desde el que corre la sesión: el inicio real si el docente la inició, o el programado. */
    public LocalDateTime inicioEfectivo() {
        return fechaInicioReal != null ? fechaInicioReal : fechaInicio;
    }
}
