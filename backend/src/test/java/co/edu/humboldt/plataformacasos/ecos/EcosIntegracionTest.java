package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Contrato ecos-soma/1 de punta a punta: catálogo, código de sesión, canje, eventos y resultado recalculado.
 * El catálogo es SINTÉTICO (4 preguntas, 100 puntos): comprueba el flujo, no los puntos reales del juego.
 */
@SpringBootTest(properties = "pruebas.contexto=ecos")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EcosIntegracionTest {

    private static final String VERSION = "0123456789abcdef0123456789abcdef";
    private static final String ANA = "ana.perez@institucion.edu.co";
    private static final String LUIS = "luis.rios@institucion.edu.co";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    // ---------------------------------------------------------------- ayudas

    private record Resp(int estado, JsonNode cuerpo) {
        String codigo() { return cuerpo.has("codigo") ? cuerpo.get("codigo").asString() : null; }
    }

    private Resp enviar(String ruta, String token, Object cuerpo) throws Exception {
        var req = post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo == null ? "" : json.writeValueAsString(cuerpo));
        if (token != null) { req = req.header("Authorization", "Bearer " + token); }
        return leer(mockMvc.perform(req).andReturn());
    }

    private Resp pedir(String ruta, String token) throws Exception {
        return leer(mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token)).andReturn());
    }

    private Resp leer(MvcResult r) throws Exception {
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Resp(r.getResponse().getStatus(), texto.isBlank() ? json.createObjectNode() : json.readTree(texto));
    }

    private String tokenWeb(String correo, String clave, boolean estudiante) throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        return estudiante
                ? s.token(s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante(correo, clave)))
                : s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private String ana() throws Exception { return tokenWeb(ANA, "CODIGO_VALIDO_01", true); }
    private String luis() throws Exception { return tokenWeb(LUIS, "CODIGO_VALIDO_02", true); }
    private String admin() throws Exception { return tokenWeb("admin.plataforma@institucion.edu.co", "Clave-Admin-01", false); }

    private static Map<String, Object> opcion(String id, String grado, double credito) {
        return Map.of("id", id, "grado", grado, "credito", credito, "texto", "Opción " + id, "retroalimentacion", "r");
    }

    private static Map<String, Object> pregunta(String id, int puntos, List<String> criterios, List<Map<String, Object>> opciones) {
        return Map.of("id", id, "escenario", "E1", "nucleo", true, "puntos", puntos, "enunciado", "Enunciado " + id,
                "criterios", criterios, "opciones", opciones);
    }

    private static Map<String, Object> catalogo() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("casoVersion", VERSION); c.put("casoId", "CasoSintetico"); c.put("nombre", "Caso sintético"); c.put("creditoParcial", true);
        c.put("criterios", List.of(Map.of("id", "C1", "nombre", "Criterio 1"), Map.of("id", "C2", "nombre", "Criterio 2"), Map.of("id", "C3", "nombre", "Criterio 3")));
        c.put("preguntas", List.of(
                pregunta("P1", 40, List.of("C1"), List.of(opcion("A", "Correct", 1), opcion("B", "Partial", 0.5), opcion("C", "Incorrect", 0))),
                pregunta("P2", 30, List.of("C1", "C2"), List.of(opcion("A", "Correct", 1), opcion("B", "Partial", 0.4), opcion("C", "Incorrect", 0))),
                pregunta("P3", 20, List.of("C2"), List.of(opcion("A", "Correct", 1), opcion("B", "Incorrect", 0))),
                pregunta("P4", 10, List.of("C3"), List.of(opcion("A", "Correct", 1), opcion("B", "Incorrect", 0)))));
        return c;
    }

    private void cargarCatalogo() throws Exception {
        assertThat(enviar("/api/ecos/v1/catalogo", admin(), catalogo()).estado()).isIn(200, 201);
    }

    /** Crea la sesión como estudiante y la canjea como el juego. */
    private Resp[] iniciar(String tokenWeb) throws Exception {
        Resp creada = enviar("/api/ecos/v1/sesiones", tokenWeb, null);
        assertThat(creada.estado()).isEqualTo(201);
        Resp canje = enviar("/api/ecos/v1/sesiones/" + creada.cuerpo().get("codigo").asString() + "/canje", null, null);
        assertThat(canje.estado()).isEqualTo(200);
        return new Resp[] {creada, canje};
    }

    private Map<String, Object> envio(Resp canje, Map<String, String> respuestas, Object notaJuego, double segundos) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (var e : respuestas.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("preguntaId", e.getKey()); r.put("opcionId", e.getValue());
            lista.add(r);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contrato", "ecos-soma/1"); m.put("envioId", UUID.randomUUID().toString());
        m.put("sesionId", canje.cuerpo().get("sesionId").asString()); m.put("intento", 1);
        m.put("caso", Map.of("id", "CasoSintetico", "version", canje.cuerpo().get("caso").get("version").asString()));
        m.put("juego", Map.of("build", "prueba", "motor", "5.8"));
        m.put("inicio", "2026-11-24T09:16:14-05:00"); m.put("fin", "2026-11-24T09:17:14-05:00");
        m.put("segundosParticipacion", segundos); m.put("protagonista", "Andrea");
        m.put("respuestas", lista); m.put("desenlace", "FIN_SOSTENIDA"); m.put("indicadores", Map.of("medidas", 4));
        if (notaJuego != null) { m.put("calculoJuego", Map.of("puntaje", 60.0, "posible", 100.0, "nota", notaJuego, "nivel", "Básico")); }
        return m;
    }

    private static Map<String, String> respuestasMixtas() {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("P1", "B"); r.put("P2", "A"); r.put("P3", null); r.put("P4", "A");   // 20 + 30 + 0 + 10 = 60 → 3,0
        return r;
    }

    private static String rutaResultado(Resp canje) { return "/api/ecos/v1/sesiones/" + canje.cuerpo().get("sesionId").asString() + "/resultado"; }
    private static String tokenJuego(Resp canje) { return canje.cuerpo().get("token").asString(); }

    // ---------------------------------------------------------------- pruebas

    @Test
    @Order(1)
    @DisplayName("Sin catálogo cargado nadie puede crear una sesión")
    void sinCatalogo() throws Exception {
        Resp r = enviar("/api/ecos/v1/sesiones", ana(), null);
        assertThat(r.estado()).isEqualTo(409);
        assertThat(r.codigo()).isEqualTo("SIN_CATALOGO");
    }

    @Test
    @Order(2)
    @DisplayName("El catálogo solo lo carga el administrador, es idempotente y se valida")
    void cargaDeCatalogo() throws Exception {
        assertThat(enviar("/api/ecos/v1/catalogo", tokenWeb("laura.mejia@institucion.edu.co", "Clave-Docente-02", false), catalogo()).estado()).isEqualTo(403);
        assertThat(enviar("/api/ecos/v1/catalogo", admin(), catalogo()).estado()).isEqualTo(201);
        assertThat(enviar("/api/ecos/v1/catalogo", admin(), catalogo()).estado()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ecos_pregunta WHERE caso_version = ?", Integer.class, VERSION)).isEqualTo(4);

        Map<String, Object> malo = catalogo();
        malo.put("casoVersion", "ffffffffffffffffffffffffffffffff");
        @SuppressWarnings("unchecked") List<Map<String, Object>> ps = (List<Map<String, Object>>) malo.get("preguntas");
        malo.put("preguntas", List.of(pregunta("P1", 10, List.of("C1"), List.of(opcion("A", "Correct", 0.3), opcion("B", "Incorrect", 0)))));
        assertThat(enviar("/api/ecos/v1/catalogo", admin(), malo).estado()).isEqualTo(400);
        assertThat(ps).hasSize(4);
    }

    @Test
    @Order(3)
    @DisplayName("Flujo completo: código de un solo uso, canje, eventos y resultado recalculado por SOMA")
    void flujoCompleto() throws Exception {
        cargarCatalogo();
        String web = ana();
        assertThat(pedir("/api/ecos/v1/mis-intentos", web).cuerpo().get("disponible").asBoolean()).isTrue();

        // Un solo código vigente: al pedir otro, el anterior deja de servir.
        Resp primera = enviar("/api/ecos/v1/sesiones", web, null);
        String codigo1 = primera.cuerpo().get("codigo").asString();
        assertThat(codigo1).matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$");
        Resp creada = enviar("/api/ecos/v1/sesiones", web, null);
        String codigo = creada.cuerpo().get("codigo").asString();
        assertThat(creada.cuerpo().get("enlace").asString()).startsWith("ecos://iniciar?sesion=" + codigo + "&api=");
        Resp vencido = enviar("/api/ecos/v1/sesiones/" + codigo1 + "/canje", null, null);
        assertThat(vencido.estado()).isEqualTo(410);
        assertThat(vencido.codigo()).isEqualTo("SESION_VENCIDA");
        assertThat(enviar("/api/ecos/v1/sesiones/ZZZZ-ZZZZ/canje", null, null).codigo()).isEqualTo("SESION_NO_EXISTE");
        assertThat(enviar("/api/ecos/v1/sesiones/mal-codigo/canje", null, null).estado()).isEqualTo(404);

        Resp canje = enviar("/api/ecos/v1/sesiones/" + codigo + "/canje", null, null);
        assertThat(canje.estado()).isEqualTo(200);
        assertThat(canje.cuerpo().get("contrato").asString()).isEqualTo("ecos-soma/1");
        assertThat(canje.cuerpo().get("caso").get("version").asString()).isEqualTo(VERSION);
        assertThat(canje.cuerpo().get("reanudar").asBoolean()).isFalse();
        assertThat(canje.cuerpo().get("estudiante").get("nombreVisible").asString()).isEqualTo("Ana P.");
        Resp repetido = enviar("/api/ecos/v1/sesiones/" + codigo + "/canje", null, null);
        assertThat(repetido.estado()).isEqualTo(409);
        assertThat(repetido.codigo()).isEqualTo("SESION_YA_USADA");

        // Eventos: solo con el token del canje.
        String ruta = "/api/ecos/v1/sesiones/" + canje.cuerpo().get("sesionId").asString();
        Map<String, Object> evento = Map.of("envioId", UUID.randomUUID().toString(), "punto", "Acto2_Fin", "acto", 2, "fecha", "2026-11-24T09:20:00-05:00");
        assertThat(enviar(ruta + "/eventos", tokenJuego(canje), evento).estado()).isEqualTo(202);
        assertThat(jdbc.queryForObject("SELECT ultimo_punto FROM ecos_sesion WHERE codigo = ?", String.class, codigo)).isEqualTo("Acto2_Fin");
        assertThat(enviar(ruta + "/eventos", null, evento).estado()).isEqualTo(401);
        assertThat(enviar(ruta + "/eventos", web, evento).codigo()).isEqualTo("TOKEN_INVALIDO");          // el JWT web no sirve aquí
        assertThat(enviar(ruta + "/eventos", "basura", evento).estado()).isEqualTo(401);
        Resp deLuis = iniciar(luis())[1];
        assertThat(enviar(ruta + "/eventos", tokenJuego(deLuis), evento).estado()).isEqualTo(401);          // token de otra sesión
        // ...y el token del juego tampoco abre el resto de la API.
        assertThat(pedir("/api/avisos", tokenJuego(canje)).estado()).isEqualTo(401);

        // Resultado: SOMA recalcula 20 + 30 + 0 + 10 = 60 → 3,0 Básico.
        Map<String, Object> envio = envio(canje, respuestasMixtas(), 3.0, 1.5);
        Resp recibido = enviar(ruta + "/resultado", tokenJuego(canje), envio);
        assertThat(recibido.estado()).isEqualTo(201);
        assertThat(recibido.cuerpo().get("nota").asDouble()).isEqualTo(3.0);
        assertThat(recibido.cuerpo().get("nivel").asString()).isEqualTo("Básico");
        assertThat(recibido.cuerpo().get("puntaje").asDouble()).isEqualTo(60.0);
        assertThat(recibido.cuerpo().get("revision").asBoolean()).isFalse();
        assertThat(recibido.cuerpo().get("fortalezas").toString()).isEqualTo("[\"C3\"]");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ecos_respuesta WHERE envio_id = ?", Integer.class, envio.get("envioId"))).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ecos_resultado_criterio WHERE envio_id = ?", Integer.class, envio.get("envioId"))).isEqualTo(3);

        // Idempotencia: el mismo envioId devuelve 200 sin duplicar; otro envío a una sesión cerrada es 409.
        assertThat(enviar(ruta + "/resultado", tokenJuego(canje), envio).estado()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ecos_resultado WHERE sesion_id = ?", Integer.class, canje.cuerpo().get("sesionId").asString())).isEqualTo(1);
        Resp otro = enviar(ruta + "/resultado", tokenJuego(canje), envio(canje, respuestasMixtas(), 3.0, 1.5));
        assertThat(otro.estado()).isEqualTo(409);
        assertThat(otro.codigo()).isEqualTo("SESION_CERRADA");
        assertThat(enviar(ruta + "/eventos", tokenJuego(canje), evento).codigo()).isEqualTo("SESION_CERRADA");

        // Se acabó el único intento.
        JsonNode estado = pedir("/api/ecos/v1/mis-intentos", web).cuerpo();
        assertThat(estado.get("disponible").asBoolean()).isFalse();
        assertThat(estado.get("usados").asInt()).isEqualTo(1);
        assertThat(estado.get("intentos").get(0).get("nota").asDouble()).isEqualTo(3.0);
        assertThat(enviar("/api/ecos/v1/sesiones", web, null).codigo()).isEqualTo("INTENTOS_AGOTADOS");
    }

    @Test
    @Order(4)
    @DisplayName("SOMA rechaza envíos inválidos y marca para revisión cuando la nota del juego difiere")
    void validaciones() throws Exception {
        cargarCatalogo();
        // La sesión que Luis dejó canjeada en la prueba anterior ocupa su único intento: se limpia para empezar de cero.
        jdbc.update("DELETE FROM ecos_sesion WHERE estudiante_id = (SELECT CAST(id AS CHAR) FROM usuario WHERE correo_institucional = ?)", LUIS);
        Resp canje = iniciar(luis())[1];
        String ruta = rutaResultado(canje);
        String t = tokenJuego(canje);

        Map<String, Object> distinto = envio(canje, respuestasMixtas(), null, 1.5);
        distinto.put("caso", Map.of("id", "CasoSintetico", "version", "ffffffffffffffffffffffffffffffff"));
        assertThat(enviar(ruta, t, distinto).codigo()).isEqualTo("CASO_DISTINTO");

        Map<String, String> faltan = respuestasMixtas();
        faltan.remove("P4");
        assertThat(enviar(ruta, t, envio(canje, faltan, null, 1.5)).codigo()).isEqualTo("VALIDACION");
        Map<String, String> invalida = respuestasMixtas();
        invalida.put("P1", "Z");
        assertThat(enviar(ruta, t, envio(canje, invalida, null, 1.5)).codigo()).isEqualTo("VALIDACION");
        assertThat(enviar(ruta, t, envio(canje, respuestasMixtas(), null, 99999)).codigo()).isEqualTo("VALIDACION");   // más tiempo del real

        Map<String, Object> alReves = envio(canje, respuestasMixtas(), null, 1.5);
        alReves.put("fin", "2026-11-24T09:10:00-05:00");
        assertThat(enviar(ruta, t, alReves).codigo()).isEqualTo("VALIDACION");
        Map<String, Object> sinSesion = envio(canje, respuestasMixtas(), null, 1.5);
        sinSesion.put("sesionId", UUID.randomUUID().toString());
        assertThat(enviar(ruta, t, sinSesion).estado()).isEqualTo(422);
        // Nada de lo anterior cerró la sesión ni guardó datos.
        assertThat(jdbc.queryForObject("SELECT estado FROM ecos_sesion WHERE sesion_id = ?", String.class, canje.cuerpo().get("sesionId").asString())).isEqualTo("CANJEADA");

        // El juego dice 5,0 pero SOMA calcula 3,0: se guarda la de SOMA y queda para revisión.
        Resp ok = enviar(ruta, t, envio(canje, respuestasMixtas(), 5.0, 1.5));
        assertThat(ok.estado()).isEqualTo(201);
        assertThat(ok.cuerpo().get("nota").asDouble()).isEqualTo(3.0);
        assertThat(ok.cuerpo().get("revision").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT nota_juego FROM ecos_resultado WHERE sesion_id = ?", Double.class, canje.cuerpo().get("sesionId").asString())).isEqualTo(5.0);
    }

    @Test
    @Order(5)
    @DisplayName("Un código vencido no se puede canjear")
    void codigoVencido() throws Exception {
        cargarCatalogo();
        // Ningún estudiante nuevo disponible: se reutiliza a Ana (ya sin intentos) solo para el caso de expiración directa en la base.
        String sesion = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO ecos_sesion (sesion_id, codigo, estudiante_id, caso_version, intento, estado, creada_en, vence_en) VALUES (?,?,?,?,?,?,?,?)",
                sesion, "AAAA-2222", "1", VERSION, 1, "CREADA", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusHours(2)),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusHours(1)));
        Resp r = enviar("/api/ecos/v1/sesiones/AAAA-2222/canje", null, null);
        assertThat(r.estado()).isEqualTo(410);
        assertThat(r.codigo()).isEqualTo("SESION_VENCIDA");
        assertThat(jdbc.queryForObject("SELECT estado FROM ecos_sesion WHERE sesion_id = ?", String.class, sesion)).isEqualTo("VENCIDA");
    }
}
