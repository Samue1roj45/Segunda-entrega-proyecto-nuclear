package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;

/**
 * Persistencia del intento de envío. Un fallo SMTP no relanza la excepción
 * para no revertir retroalimentación ni calificaciones ya guardadas.
 */
@Service
public class ServicioCorreoNotificaciones {

    private final NotificacionRepositorio repositorio;
    private final CatalogoAcademico catalogo;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String remitente;

    public ServicioCorreoNotificaciones(
            NotificacionRepositorio repositorio,
            CatalogoAcademico catalogo,
            JavaMailSender mailSender,
            Clock clock,
            @Value("${app.mail.from:no-reply@institucion.edu.co}") String remitente) {
        this.repositorio = repositorio;
        this.catalogo = catalogo;
        this.mailSender = mailSender;
        this.clock = clock;
        this.remitente = remitente;
    }

    @Transactional
    public ResultadoEnvio emitir(EventoAcademico evento) {
        Optional<Notificacion> existente = repositorio.findByClaveIdempotencia(evento.claveIdempotencia());
        if (existente.isPresent()) {
            Notificacion previa = existente.get();
            if (previa.getEstado() == EstadoEnvio.ENVIADA) {
                return ResultadoEnvio.OMITIDA_DUPLICADO;
            }
            sincronizar(previa, evento);
            return intentarSmtp(previa, evento.cuerpoCorreo());
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
        try {
            n = repositorio.saveAndFlush(n);
        } catch (DataIntegrityViolationException ex) {
            return ResultadoEnvio.OMITIDA_DUPLICADO;
        }
        return intentarSmtp(n, evento.cuerpoCorreo());
    }

    private void sincronizar(Notificacion n, EventoAcademico evento) {
        EstudianteRef estudiante = catalogo.buscarEstudiante(evento.estudianteId()).orElse(null);
        n.setTipo(evento.tipo());
        n.setPracticaId(evento.practicaId());
        n.setEstudianteId(evento.estudianteId());
        n.setParticipacionId(evento.participacionId());
        if (estudiante != null && estudiante.correo() != null && !estudiante.correo().isBlank()) {
            n.setDestinatarioCorreo(estudiante.correo());
        }
        n.setAsunto(evento.asunto());
        n.setCuerpo(evento.cuerpo());
    }

    private ResultadoEnvio intentarSmtp(Notificacion n, String textoCorreo) {
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
            mensaje.setText(textoCorreo == null ? n.getCuerpo() : textoCorreo);
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
