package com.example.demo.dto;

import java.math.BigDecimal;
/*
 #REQUISITO RF-11
 Para qué sirve: progreso del estudiante (preguntas respondidas / total y %).
 Sale de la vista vw_progreso_participacion de la BD.
*/
public record ProgresoDto(Long idParticipacion, int totalPreguntas,
                          int preguntasRespondidas, BigDecimal porcentajeProgreso) {
}
