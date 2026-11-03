package com.example.demo.dto;

import java.time.LocalDateTime;
/*
 #REQUISITO RF-23
 Para qué sirve: el reloj de la participación. El tiempo corre desde que el estudiante
 inicia (hora_inicio) hasta hora_inicio + tiempo máximo. El frontend lo usa para el cronómetro.
 */
public record TiempoRestanteDto(Long idParticipacion, String estado, LocalDateTime horaInicio,
                                LocalDateTime limite, long segundosRestantes, boolean expirada) {
}
