package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.OffsetDateTime;

public record NotificacionRespuesta(
        Long id,
        TipoNotificacion tipo,
        Long practicaId,
        Long estudianteId,
        Long participacionId,
        String destinatarioCorreo,
        String asunto,
        String cuerpo,
        EstadoEnvio estado,
        String errorMensaje,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaEnvio) {
}
