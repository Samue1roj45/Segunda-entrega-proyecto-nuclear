package co.edu.humboldt.plataformacasos.preferencias;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/** Preferencias de interfaz del usuario que inició sesión: un objeto JSON pequeño de valores simples. */
@RestController
@RequestMapping("/api/preferencias")
public class PreferenciasControlador {

    private static final int MAX_CLAVES = 30;
    private static final int MAX_TEXTO = 60;

    private final JdbcTemplate jdbc;
    private final ServicioAutorizacion autorizacion;
    private final ObjectMapper json;

    public PreferenciasControlador(JdbcTemplate jdbc, ServicioAutorizacion autorizacion, ObjectMapper json) {
        this.jdbc = jdbc;
        this.autorizacion = autorizacion;
        this.json = json;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public JsonNode obtener() {
        Long yo = autorizacion.verificarIdentidad().id();
        List<String> filas = jdbc.queryForList("SELECT datos FROM usuario_preferencias WHERE usuario_id = ?", String.class, yo);
        try {
            return filas.isEmpty() ? json.createObjectNode() : json.readTree(filas.get(0));
        } catch (Exception e) {
            return json.createObjectNode();
        }
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void guardar(@RequestBody JsonNode datos) {
        Long yo = autorizacion.verificarIdentidad().id();
        validar(datos);
        int n = jdbc.update("UPDATE usuario_preferencias SET datos = ?, actualizado = ? WHERE usuario_id = ?",
                datos.toString(), Timestamp.valueOf(LocalDateTime.now()), yo);
        if (n == 0) {
            jdbc.update("INSERT INTO usuario_preferencias (usuario_id, datos, actualizado) VALUES (?, ?, ?)",
                    yo, datos.toString(), Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    /** Solo objetos planos con valores simples y cortos: no es un lugar para guardar datos arbitrarios. */
    private static void validar(JsonNode datos) {
        if (datos == null || !datos.isObject() || datos.size() > MAX_CLAVES) {
            throw new SolicitudInvalidaException("Las preferencias deben ser un objeto con pocas claves.");
        }
        datos.properties().forEach(e -> {
            JsonNode v = e.getValue();
            boolean simple = v.isNumber() || v.isBoolean() || (v.isTextual() && v.asText().length() <= MAX_TEXTO);
            if (e.getKey().length() > 40 || !simple) {
                throw new SolicitudInvalidaException("Preferencia no válida: " + e.getKey());
            }
        });
    }
}
