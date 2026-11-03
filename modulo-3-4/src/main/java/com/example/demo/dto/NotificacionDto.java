package com.example.demo.dto;

import java.time.LocalDateTime;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una notificación vista por su destinatario (bandeja de avisos).
 */
public record NotificacionDto(Long idNotificacion, Long idPractica, String tipo, String canal,
                              String asunto, String contenido, String estadoEnvio,
                              LocalDateTime fechaCreacion, boolean leida) {
}
