package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila del reporte de participación de una sesión (vw_reporte_participacion).
 */
public record ReporteParticipacionDto(Long idPractica, Long idParticipacion, Long idEstudiante, String estudiante,
                                      String correoEstudiante, Long numeroIntento, String estadoParticipacion,
                                      LocalDateTime horaInicio, LocalDateTime horaFin, BigDecimal nota,
                                      String feedbackDocente, LocalDateTime fechaCalculo,
                                      Integer preguntasRespondidas, Integer totalPreguntas,
                                      BigDecimal porcentajeProgreso) {
}
