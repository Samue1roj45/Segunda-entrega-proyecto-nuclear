package com.example.demo.dto;

import java.time.LocalDateTime;
/*
 #REQUISITO RF-23 || RF-25
 Para qué sirve: confirma que el estudiante terminó su intento. A propósito NO devuelve
 la nota (la entrega de resultados es otro requisito).
 */
public record FinalizacionDto(Long idParticipacion, String estado, LocalDateTime horaFin) {
}
