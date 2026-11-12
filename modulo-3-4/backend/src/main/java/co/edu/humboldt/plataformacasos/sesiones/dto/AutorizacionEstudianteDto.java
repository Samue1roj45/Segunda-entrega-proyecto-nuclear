package co.edu.humboldt.plataformacasos.sesiones.dto;

import java.time.LocalDateTime;

/*
 #REQUISITO RF-04 || RF-18 || RF-20
 Para qué sirve: lo que ve el ESTUDIANTE después de iniciar sesión: las sesiones a las que está
 convocado, con sus condiciones. Con idAutorizacion consulta si puede ingresar (RF-22) e inicia
 su participación (RF-12). Nunca incluye el código de acceso.
 */
public record AutorizacionEstudianteDto(Long idAutorizacion, Long idPractica, Long idCaso, String nombreCaso,
                                        String espacioAcademico, LocalDateTime fechaInicio,
                                        LocalDateTime fechaFin, int tiempoMaximoMinutos, String lugar,
                                        String mensajePersonalizado, String urlAcceso, String estadoSesion,
                                        int intentosUsados, int intentosPermitidos) {
}
