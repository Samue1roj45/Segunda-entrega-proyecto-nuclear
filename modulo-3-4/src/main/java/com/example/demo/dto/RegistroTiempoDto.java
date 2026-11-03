package com.example.demo.dto;

import java.time.LocalDateTime;
/*
 #REQUISITO RF-12
 Para qué sirve: registro de cuándo empezó y terminó una participación (y cuánto duró), junto con
 el inicio y cierre reales de la sesión. Un AUSENTE no tiene hora de inicio ni de fin.
 */
public record RegistroTiempoDto(Long idParticipacion, Long numeroIntento, Long idEstudiante,
                                String estudiante, String estadoParticipacion,
                                LocalDateTime horaInicio, LocalDateTime horaFin, Long duracionSegundos,
                                LocalDateTime sesionInicioReal, LocalDateTime sesionCierreReal,
                                String tipoCierre) {
}
