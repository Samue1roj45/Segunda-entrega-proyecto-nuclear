package co.edu.humboldt.plataformacasos.sesiones.dto;

import java.math.BigDecimal;
/*
 #REQUISITO RF-11
 Para qué sirve: progreso del estudiante (preguntas respondidas / total y %).
 Se calcula con las preguntas del caso (módulo 2) y las respuestas del intento (módulo 5).
*/
public record ProgresoDto(Long idParticipacion, int totalPreguntas,
                          int preguntasRespondidas, BigDecimal porcentajeProgreso) {
}
