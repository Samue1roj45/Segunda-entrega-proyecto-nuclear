package com.unihumboldt.nuclear.evaluacion.notificacion;

/**
 * Evento de dominio para el Observer (RF-21 y RF-27).
 */
public record EventoAcademico(
        TipoNotificacion tipo,
        String claveIdempotencia,
        Long practicaId,
        Long estudianteId,
        Long participacionId,
        String asunto,
        String cuerpo) {
}
