package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;

/**
 * Persistencia del intento de envío. Un fallo SMTP no relanza la excepción
 * para no revertir retroalimentación ni calificaciones ya guardadas.
 */
@Service
public class NotificacionCorreoServicio {

    private final NotificacionRepository repositorio;
    private final CatalogoAcademico catalogo;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String remitente;

    public NotificacionCorreoServicio(
            NotificacionRepository repositorio,
            CatalogoAcademico catalogo,
            JavaMailSender mailSender,
            Clock clock,
            @Value("${app.mail.from:no-reply@simulador.local}") String remitente) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.mailSender = mailSender;
        this.clock = clock;
        this.remitente = remitente;
    }

    @Transactional
    public ResultadoEnvio emitir(EventoAcademico evento) {
        if (repositorio.existsByClaveIdempotencia(evento.claveIdempotencia())) {
            return ResultadoEnvio.OMITIDA_DUPLICADO;
        }
        EstudianteRef estudiante = catalogo.buscarEstudiante(evento.estudianteId()).orElse(null);
        Notificacion n = new Notificacion();
        n.setTipo(evento.tipo());
        n.setClaveIdempotencia(evento.claveIdempotencia());
        n.setPracticaId(evento.practicaId());
        n.setEstudianteId(evento.estudianteId());
        n.setParticipacionId(evento.participacionId());
        n.setDestinatarioCorreo(estudiante == null ? null : estudiante.correo());
        n.setAsunto(evento.asunto());
        n.setCuerpo(evento.cuerpo());
        n.setEstado(EstadoEnvio.PENDIENTE);
        n.setFechaCreacion(OffsetDateTime.now(clock));
        n = repositorio.save(n);
        return intentarSmtp(n);
    }

    private ResultadoEnvio intentarSmtp(Notificacion n) {
        if (n.getDestinatarioCorreo() == null || n.getDestinatarioCorreo().isBlank()) {
            n.setEstado(EstadoEnvio.ERROR);
            n.setErrorMensaje("Sin correo en el catálogo académico; no se inventó destinatario.");
            repositorio.save(n);
            return ResultadoEnvio.ERROR;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(n.getDestinatarioCorreo());
            mensaje.setSubject(n.getAsunto());
            mensaje.setText(n.getCuerpo());
            mailSender.send(mensaje);
            n.setEstado(EstadoEnvio.ENVIADA);
            n.setFechaEnvio(OffsetDateTime.now(clock));
            n.setErrorMensaje(null);
            repositorio.save(n);
            return ResultadoEnvio.ENVIADA;
        } catch (MailException ex) {
            n.setEstado(EstadoEnvio.ERROR);
            n.setErrorMensaje(ex.getMessage() == null ? "Error SMTP" : recortar(ex.getMessage()));
            repositorio.save(n);
            return ResultadoEnvio.ERROR;
        }
    }

    private static String recortar(String mensaje) {
        return mensaje.length() <= 500 ? mensaje : mensaje.substring(0, 500);
    }
}
