package co.edu.humboldt.plataformacasos.configuracion;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorreoBrevoTest {

    private HttpServer servidor;
    private final List<String> cuerpos = new ArrayList<>();
    private final List<String> claves = new ArrayList<>();
    private int estado = 201;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v3/smtp/email", intercambio -> {
            cuerpos.add(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            claves.add(intercambio.getRequestHeaders().getFirst("api-key"));
            byte[] respuesta = "{\"messageId\":\"x\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado, respuesta.length);
            intercambio.getResponseBody().write(respuesta);
            intercambio.close();
        });
        servidor.start();
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private CorreoBrevo.EnviadorBrevo enviador() {
        URI url = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email");
        return new CorreoBrevo.EnviadorBrevo("clave-prueba", url, "SOMA");
    }

    @Test
    void enviaUnCorreoSimplePorLaApi() {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom("remitente@unihumboldt.edu.co");
        m.setTo("ana.perez@institucion.edu.co");
        m.setSubject("SOMA · Código de acceso");
        m.setText("Tu código es \"ABC\"\nGracias");

        enviador().send(m);

        assertThat(claves).containsExactly("clave-prueba");
        String json = cuerpos.get(0);
        assertThat(json).contains("\"sender\":{\"email\":\"remitente@unihumboldt.edu.co\",\"name\":\"SOMA\"}");
        assertThat(json).contains("\"to\":[{\"email\":\"ana.perez@institucion.edu.co\"}]");
        assertThat(json).contains("\"subject\":\"SOMA · Código de acceso\"");
        assertThat(json).contains("\"textContent\":\"Tu código es \\\"ABC\\\"\\nGracias\"");
    }

    @Test
    void enviaTextoYHtmlDeUnCorreoMime() throws Exception {
        CorreoBrevo.EnviadorBrevo enviador = enviador();
        var mensaje = enviador.createMimeMessage();
        var ayuda = new MimeMessageHelper(mensaje, true, "UTF-8");
        ayuda.setFrom("remitente@unihumboldt.edu.co");
        ayuda.setTo("luis.rios@institucion.edu.co");
        ayuda.setSubject("SOMA · Te invitaron");
        ayuda.setText("plano", "<p>html</p>");

        enviador.send(mensaje);

        String json = cuerpos.get(0);
        assertThat(json).contains("\"htmlContent\":\"<p>html</p>\"");
        assertThat(json).contains("\"textContent\":\"plano\"");
        assertThat(json).contains("\"to\":[{\"email\":\"luis.rios@institucion.edu.co\"}]");
    }

    @Test
    void unErrorDeBrevoSeReportaComoFalloDeEnvio() {
        estado = 401;
        SimpleMailMessage m = new SimpleMailMessage();
        m.setFrom("remitente@unihumboldt.edu.co");
        m.setTo("ana.perez@institucion.edu.co");
        m.setSubject("x");
        m.setText("y");

        assertThatThrownBy(() -> enviador().send(m)).isInstanceOf(MailSendException.class);
    }
}
