package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Pixel Streaming: el estudiante recibe el enlace del reproductor y ve el avance que el juego informa de su sesión.
 * El catálogo es SINTÉTICO (una pregunta): aquí solo importa el flujo código → canje → evento → avance.
 */
@SpringBootTest(properties = {"pruebas.contexto=pixel-streaming",
        "app.ecos.pixel-streaming.url=https://juego.ejemplo.edu.co:8443/reproductor"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PixelStreamingIntegracionTest {

    private static final String VERSION = "fedcba9876543210fedcba9876543210";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;

    private record Resp(int estado, JsonNode cuerpo) {
    }

    private Resp leer(MvcResult r) throws Exception {
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Resp(r.getResponse().getStatus(), texto.isBlank() ? json.createObjectNode() : json.readTree(texto));
    }

    private Resp pedir(String ruta, String token) throws Exception {
        var req = get(ruta);
        if (token != null) { req = req.header("Authorization", "Bearer " + token); }
        return leer(mockMvc.perform(req).andReturn());
    }

    private Resp enviar(String ruta, String token, Object cuerpo) throws Exception {
        var req = post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo == null ? "" : json.writeValueAsString(cuerpo));
        if (token != null) { req = req.header("Authorization", "Bearer " + token); }
        return leer(mockMvc.perform(req).andReturn());
    }

    private String estudiante(String correo, String codigo) throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        return s.token(s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante(correo, codigo)));
    }

    private String personal(String correo, String clave) throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        return s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private void cargarCatalogo() throws Exception {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("casoVersion", VERSION); c.put("casoId", "CasoStreaming"); c.put("nombre", "Caso de streaming"); c.put("creditoParcial", true);
        c.put("criterios", List.of(Map.of("id", "C1", "nombre", "Criterio 1")));
        c.put("preguntas", List.of(Map.of("id", "P1", "escenario", "E1", "nucleo", true, "puntos", 10, "enunciado", "Enunciado",
                "criterios", List.of("C1"), "opciones", List.of(
                        Map.of("id", "A", "grado", "Correct", "credito", 1, "texto", "A", "retroalimentacion", "r"),
                        Map.of("id", "B", "grado", "Incorrect", "credito", 0, "texto", "B", "retroalimentacion", "r")))));
        assertThat(enviar("/api/ecos/v1/catalogo", personal("admin.plataforma@institucion.edu.co", "Clave-Admin-01"), c).estado())
                .isIn(200, 201);
    }

    @Test
    @DisplayName("El estudiante recibe el enlace del reproductor y su origen; el docente y el anónimo no")
    void configuracion() throws Exception {
        Resp ana = pedir("/api/ecos/v1/pixel-streaming", estudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01"));
        assertThat(ana.estado()).isEqualTo(200);
        assertThat(ana.cuerpo().get("activo").asBoolean()).isTrue();
        assertThat(ana.cuerpo().get("url").asString()).isEqualTo("https://juego.ejemplo.edu.co:8443/reproductor");
        assertThat(ana.cuerpo().get("origen").asString()).isEqualTo("https://juego.ejemplo.edu.co:8443");

        assertThat(pedir("/api/ecos/v1/pixel-streaming", personal("carlos.gomez@institucion.edu.co", "Clave-Docente-01")).estado())
                .isEqualTo(403);
        assertThat(pedir("/api/ecos/v1/pixel-streaming", null).estado()).isEqualTo(401);
    }

    @Test
    @DisplayName("El avance refleja lo que el juego informa, y solo lo ve el dueño de la sesión")
    void avanceDeLaSesion() throws Exception {
        cargarCatalogo();
        String ana = estudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01");
        Resp creada = enviar("/api/ecos/v1/sesiones", ana, null);
        assertThat(creada.estado()).isEqualTo(201);
        String sesionId = creada.cuerpo().get("sesionId").asString();
        String ruta = "/api/ecos/v1/sesiones/" + sesionId + "/avance";

        Resp antes = pedir(ruta, ana);
        assertThat(antes.estado()).isEqualTo(200);
        assertThat(antes.cuerpo().get("estado").asString()).isEqualTo("CREADA");
        assertThat(antes.cuerpo().get("ultimoActo").isNull()).isTrue();

        // El juego (dentro del servidor de streaming) canjea el código e informa en qué va.
        Resp canje = enviar("/api/ecos/v1/sesiones/" + creada.cuerpo().get("codigo").asString() + "/canje", null, null);
        assertThat(canje.estado()).isEqualTo(200);
        String tokenJuego = canje.cuerpo().get("token").asString();
        assertThat(enviar("/api/ecos/v1/sesiones/" + sesionId + "/eventos", tokenJuego,
                Map.of("envioId", "11111111-1111-4111-8111-111111111111", "punto", "E1_Q1", "acto", 2)).estado()).isEqualTo(202);

        Resp despues = pedir(ruta, ana);
        assertThat(despues.cuerpo().get("estado").asString()).isEqualTo("CANJEADA");
        assertThat(despues.cuerpo().get("ultimoPunto").asString()).isEqualTo("E1_Q1");
        assertThat(despues.cuerpo().get("ultimoActo").asInt()).isEqualTo(2);
        assertThat(despues.cuerpo().get("ultimoEventoEn").isNull()).isFalse();

        // Otro estudiante no puede ver la sesión de Ana, ni saber si existe.
        assertThat(pedir(ruta, estudiante("luis.rios@institucion.edu.co", "CODIGO_VALIDO_02")).estado()).isEqualTo(404);
        assertThat(pedir("/api/ecos/v1/sesiones/no-existe/avance", ana).estado()).isEqualTo(404);
    }

    @Test
    @DisplayName("Solo un enlace http(s) absoluto activa el streaming")
    void soloEnlacesSeguros() {
        assertThat(PixelStreamingControlador.configuracionDe("").activo()).isFalse();
        assertThat(PixelStreamingControlador.configuracionDe("   ").activo()).isFalse();
        assertThat(PixelStreamingControlador.configuracionDe("javascript:alert(1)").activo()).isFalse();
        assertThat(PixelStreamingControlador.configuracionDe("ecos://iniciar").activo()).isFalse();
        assertThat(PixelStreamingControlador.configuracionDe("https://usuario:clave@host/").activo()).isFalse();
        assertThat(PixelStreamingControlador.configuracionDe("/relativa").activo()).isFalse();
        var ok = PixelStreamingControlador.configuracionDe(" http://localhost:8090/ ");
        assertThat(ok.activo()).isTrue();
        assertThat(ok.url()).isEqualTo("http://localhost:8090/");
        assertThat(ok.origen()).isEqualTo("http://localhost:8090");
    }
}
