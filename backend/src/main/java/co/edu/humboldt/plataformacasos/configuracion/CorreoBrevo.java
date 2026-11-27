package co.edu.humboldt.plataformacasos.configuracion;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de correos por la API web de Brevo (HTTPS) en lugar de SMTP. Render, en el plan gratis, bloquea la salida por
 * los puertos SMTP (25, 465 y 587), así que con {@code BREVO_API_KEY} todos los correos de SOMA (invitaciones, avisos,
 * códigos de acceso y notificaciones) salen por {@code https://api.brevo.com/v3/smtp/email}.
 *
 * <p>Sin la variable no se crea este bean y Spring Boot configura el envío SMTP de siempre ({@code MAIL_HOST} y
 * compañía). Los servicios no cambian: siguen usando {@link JavaMailSender}; aquí solo se reemplaza el transporte.
 * El remitente es {@code MAIL_FROM} y debe estar verificado como remitente en Brevo.
 */
@Configuration
@ConditionalOnExpression("!'${BREVO_API_KEY:}'.isBlank()")
class CorreoBrevo {

    @Bean
    JavaMailSender javaMailSender(@Value("${BREVO_API_KEY}") String clave,
                                  @Value("${BREVO_URL:https://api.brevo.com/v3/smtp/email}") String url,
                                  @Value("${MAIL_NOMBRE_REMITENTE:SOMA}") String nombreRemitente) {
        return new EnviadorBrevo(clave.trim(), URI.create(url), nombreRemitente);
    }

    /** Hereda de JavaMailSenderImpl para reutilizar la conversión de SimpleMailMessage a MimeMessage. */
    static class EnviadorBrevo extends JavaMailSenderImpl {

        private static final Logger LOG = LoggerFactory.getLogger(EnviadorBrevo.class);

        private final String clave;
        private final URI url;
        private final String nombreRemitente;
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        EnviadorBrevo(String clave, URI url, String nombreRemitente) {
            this.clave = clave;
            this.url = url;
            this.nombreRemitente = nombreRemitente;
            LOG.info("Correo: se envía por la API de Brevo (no por SMTP).");
        }

        @Override
        protected void doSend(MimeMessage[] mensajes, Object[] originales) {
            Map<Object, Exception> fallidos = new LinkedHashMap<>();
            for (int i = 0; i < mensajes.length; i++) {
                try {
                    enviar(mensajes[i]);
                } catch (Exception e) {
                    fallidos.put(originales != null ? originales[i] : mensajes[i], e);
                }
            }
            if (!fallidos.isEmpty()) {
                throw new MailSendException(fallidos);
            }
        }

        private void enviar(MimeMessage mensaje) throws MessagingException, IOException, InterruptedException {
            String json = json(mensaje);
            HttpRequest solicitud = HttpRequest.newBuilder(url)
                    .timeout(Duration.ofSeconds(20))
                    .header("api-key", clave)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                throw new MessagingException("Brevo respondió " + respuesta.statusCode() + ": " + respuesta.body());
            }
        }

        String json(MimeMessage mensaje) throws MessagingException, IOException {
            mensaje.saveChanges(); // fija los Content-Type de cada parte, como haría el envío SMTP
            Address[] de = mensaje.getFrom();
            if (de == null || de.length == 0) {
                throw new MessagingException("El correo no tiene remitente (MAIL_FROM).");
            }
            InternetAddress remitente = (InternetAddress) de[0];
            String nombre = remitente.getPersonal() != null ? remitente.getPersonal() : nombreRemitente;

            StringBuilder sb = new StringBuilder("{\"sender\":{\"email\":").append(texto(remitente.getAddress()))
                    .append(",\"name\":").append(texto(nombre)).append('}');
            agregarDestinatarios(sb, "to", mensaje.getRecipients(Message.RecipientType.TO));
            agregarDestinatarios(sb, "cc", mensaje.getRecipients(Message.RecipientType.CC));
            agregarDestinatarios(sb, "bcc", mensaje.getRecipients(Message.RecipientType.BCC));
            Address[] responder = mensaje.getReplyTo();
            if (responder != null && responder.length > 0 && !responder[0].equals(remitente)) {
                sb.append(",\"replyTo\":{\"email\":").append(texto(((InternetAddress) responder[0]).getAddress())).append('}');
            }
            sb.append(",\"subject\":").append(texto(mensaje.getSubject() == null ? "" : mensaje.getSubject()));

            String[] cuerpo = new String[2]; // [0] texto plano, [1] HTML
            leerCuerpo(mensaje, cuerpo);
            if (cuerpo[1] != null) sb.append(",\"htmlContent\":").append(texto(cuerpo[1]));
            if (cuerpo[0] != null) sb.append(",\"textContent\":").append(texto(cuerpo[0]));
            if (cuerpo[0] == null && cuerpo[1] == null) sb.append(",\"textContent\":\" \"");
            return sb.append('}').toString();
        }

        private static void agregarDestinatarios(StringBuilder sb, String campo, Address[] direcciones) {
            if (direcciones == null || direcciones.length == 0) return;
            List<String> lista = new ArrayList<>();
            for (Address d : direcciones) {
                lista.add("{\"email\":" + texto(((InternetAddress) d).getAddress()) + "}");
            }
            sb.append(",\"").append(campo).append("\":[").append(String.join(",", lista)).append(']');
        }

        /** Recorre las partes del mensaje (también anidadas) y guarda la primera parte de texto y la primera HTML. */
        private static void leerCuerpo(Part parte, String[] cuerpo) throws MessagingException, IOException {
            // El tipo del mensaje raíz solo se actualiza al guardar; por eso se mira primero el contenido real.
            Object contenido = parte.getContent();
            if (contenido instanceof Multipart multi) {
                for (int i = 0; i < multi.getCount(); i++) {
                    BodyPart hija = multi.getBodyPart(i);
                    leerCuerpo(hija, cuerpo);
                }
            } else if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) {
                return; // los adjuntos no se envían por esta vía
            } else if (parte.isMimeType("text/html")) {
                if (cuerpo[1] == null) cuerpo[1] = String.valueOf(contenido);
            } else if (contenido instanceof String s) {
                if (cuerpo[0] == null) cuerpo[0] = s;
            }
        }

        /** Cadena JSON con comillas y caracteres escapados. */
        static String texto(String valor) {
            StringBuilder sb = new StringBuilder(valor.length() + 16).append('"');
            for (int i = 0; i < valor.length(); i++) {
                char c = valor.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            return sb.append('"').toString();
        }
    }
}
