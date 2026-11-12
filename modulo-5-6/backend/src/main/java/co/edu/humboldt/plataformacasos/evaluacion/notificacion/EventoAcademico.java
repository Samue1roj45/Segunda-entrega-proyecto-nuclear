package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

/**
 * Evento de dominio para el Observer (RF-21 y RF-27).
 *
 * @param cuerpo       texto que se guarda en la notificación (lo ve el estudiante en GET /api/notificaciones).
 * @param cuerpoCorreo texto que se envía por correo. Solo difiere de {@code cuerpo} cuando el correo lleva un
 *                     dato que no debe quedar guardado, como el código de acceso de la convocatoria (RF-20).
 */
public record EventoAcademico(
        TipoNotificacion tipo,
        String claveIdempotencia,
        Long practicaId,
        Long estudianteId,
        Long participacionId,
        String asunto,
        String cuerpo,
        String cuerpoCorreo) {

    public EventoAcademico(TipoNotificacion tipo, String claveIdempotencia, Long practicaId, Long estudianteId,
                           Long participacionId, String asunto, String cuerpo) {
        this(tipo, claveIdempotencia, practicaId, estudianteId, participacionId, asunto, cuerpo, cuerpo);
    }

    @Override
    public String toString() {
        return "EventoAcademico[tipo=" + tipo + ", clave=" + claveIdempotencia + ", practicaId=" + practicaId
                + ", estudianteId=" + estudianteId + "]";
    }
}
