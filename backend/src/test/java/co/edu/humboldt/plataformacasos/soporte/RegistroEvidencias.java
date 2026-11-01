package co.edu.humboldt.plataformacasos.soporte;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Guarda la solicitud y la respuesta REAL de cada caso de prueba en target/evidencias/<caso>.json.
 * Esos archivos son la fuente de la "Respuesta del sistema" del documento PT-DCP-01.
 * Los tokens se recortan para no dejar credenciales completas en las evidencias.
 */
public final class RegistroEvidencias {

    private static final Path CARPETA = Path.of("target", "evidencias");
    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ObjectNode evidencia;
    private final ArrayNode pasos;
    private final String caso;

    private RegistroEvidencias(String caso, String descripcion) {
        this.caso = caso;
        this.evidencia = JSON.createObjectNode()
                .put("caso", caso)
                .put("descripcion", descripcion)
                .put("ejecutado", LocalDateTime.now().withNano(0).toString());
        this.pasos = evidencia.putArray("pasos");
    }

    public static RegistroEvidencias para(String caso, String descripcion) {
        return new RegistroEvidencias(caso, descripcion);
    }

    public RegistroEvidencias paso(String metodo, String ruta, String cuerpoSolicitud, MvcResult resultado) {
        MockHttpServletResponse respuesta = resultado.getResponse();
        ObjectNode paso = pasos.addObject();
        paso.putObject("solicitud").put("metodo", metodo).put("ruta", ruta).put("cuerpo", cuerpoSolicitud);
        ObjectNode r = paso.putObject("respuesta");
        r.put("estado", respuesta.getStatus());
        try {
            String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
            r.set("cuerpo", cuerpo.isBlank() ? JSON.nullNode() : recortarTokens(JSON.readTree(cuerpo)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public void guardar() {
        try {
            Files.createDirectories(CARPETA);
            JSON.writeValue(CARPETA.resolve(caso + ".json").toFile(), evidencia);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static com.fasterxml.jackson.databind.JsonNode recortarTokens(com.fasterxml.jackson.databind.JsonNode nodo) {
        if (nodo instanceof ObjectNode objeto && objeto.hasNonNull("token")) {
            String token = objeto.get("token").asText();
            objeto.put("token", token.substring(0, Math.min(16, token.length())) + "…(recortado)");
        }
        return nodo;
    }
}
