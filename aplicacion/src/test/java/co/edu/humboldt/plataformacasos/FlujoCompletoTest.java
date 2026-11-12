package co.edu.humboldt.plataformacasos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Prueba de punta a punta del backend único: un caso real recorre los seis módulos sin dobles de prueba
 * (solo el servidor de correo se simula para leer el código de acceso que recibe el estudiante).
 *
 * <ol>
 *   <li>Módulo 1 (RF-01 a RF-03): el administrador asigna permisos a Carlos; todos inician sesión.</li>
 *   <li>Módulo 2 (RF-09): Carlos consulta los casos publicados que puede programar.</li>
 *   <li>Módulo 3 (RF-04, RF-18, RF-19, RF-20): agenda la sesión, convoca a Ana y Luis (cada uno recibe su
 *       código por correo, RF-21), cambia las condiciones y la inicia.</li>
 *   <li>Módulo 4 (RF-10, RF-11, RF-22, RF-23, RF-25, RF-28): Ana ingresa con su código, ve el contexto y las
 *       herramientas, responde, cambia una respuesta y ve su progreso y su tiempo.</li>
 *   <li>Módulo 5 (RF-12 a RF-16, RF-24): Ana finaliza, se califica con la rúbrica, Carlos le concede una
 *       repetición, finaliza la sesión (intento abierto → INCOMPLETA, Luis → ausencia) y descarga el reporte.</li>
 *   <li>Módulo 6 (RF-21, RF-26, RF-27): retroalimentación, publicación de calificaciones y avisos.</li>
 * </ol>
 */
@SpringBootTest(properties = "pruebas.contexto=flujo-completo")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoCompletoTest {

    private static final Pattern CODIGO = Pattern.compile("código de acceso: ([A-Z0-9]+)");
    private static final String ANA = "ana.perez@institucion.edu.co";
    private static final String LUIS = "luis.rios@institucion.edu.co";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private JavaMailSender correo;

    @Test
    @DisplayName("Un caso recorre los seis módulos: permisos, caso, sesión, participación, nota y avisos")
    void flujoCompletoEntreModulos() throws Exception {
        // ---------- Módulo 1: permisos y sesiones de usuario ----------
        String admin = loginPersonal("admin.plataforma@institucion.edu.co", "Clave-Admin-01");
        long idCarlos = idDocente(admin, "carlos.gomez@institucion.edu.co");
        enviar(put("/api/admin/docentes/" + idCarlos + "/permisos"), admin, """
                {"permisos":["GESTIONAR_CASOS","PROGRAMAR_SESIONES","CONSULTAR_PARTICIPACIONES",
                 "DESCARGAR_REPORTES","REGISTRAR_RETROALIMENTACION","AUTORIZAR_REPETICION"]}""", 200);
        String carlos = loginPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01");

        // ---------- Módulo 2: caso publicado disponible para programar ----------
        JsonNode publicados = enviar(get("/api/integracion/casos/publicados"), carlos, null, 200);
        long idCaso = publicados.get(0).get("id").asLong();
        assertThat(publicados.get(0).get("programable").asBoolean()).isTrue();

        // ---------- Módulo 4 (RF-28): herramientas del caso ----------
        enviar(post("/api/casos/" + idCaso + "/herramientas"), carlos, """
                {"nombre":"Escala de conformidad","tipo":"TABLA","contenido":"{\\"filas\\":[1,2,3]}"}""", 201);

        // ---------- Módulo 3: agendar, convocar y cambiar condiciones ----------
        long idAna = idUsuario(ANA);
        long idLuis = idUsuario(LUIS);
        LocalDateTime inicio = LocalDateTime.now(ZoneId.of("America/Bogota")).truncatedTo(ChronoUnit.MINUTES);
        JsonNode sesion = enviar(post("/api/sesiones"), carlos, """
                {"idCaso":%d,"espacioAcademico":"Psicología Social I","fechaInicio":"%s","fechaFin":"%s",
                 "tiempoMaximoMinutos":60,"lugar":"Aula 101","idsEstudiantes":[%d,%d]}"""
                .formatted(idCaso, inicio, inicio.plusHours(2), idAna, idLuis), 201);
        long idSesion = sesion.get("idPractica").asLong();
        assertThat(sesion.get("convocados")).hasSize(2);
        String codigoAna = codigoEnviadoA(ANA);

        enviar(put("/api/sesiones/" + idSesion), carlos, """
                {"espacioAcademico":"Psicología Social I","fechaInicio":"%s","fechaFin":"%s",
                 "tiempoMaximoMinutos":60,"lugar":"Aula 204","mensajePersonalizado":"Cambió el salón"}"""
                .formatted(inicio, inicio.plusHours(2)), 200);

        // ---------- Módulo 1 + 3 (RF-01, RF-20): Ana entra con el código que le llegó ----------
        MvcResult login = loginEstudiante(ANA, codigoAna);
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        String ana = token(login);
        JsonNode convocatorias = enviar(get("/api/autorizaciones"), ana, null, 200);
        long idAutorizacion = autorizacionDe(convocatorias, idSesion);

        // RF-22: la sesión aún no ha iniciado
        assertThat(enviar(get("/api/autorizaciones/" + idAutorizacion + "/ingreso"), ana, null, 200)
                .get("permitido").asBoolean()).isFalse();
        enviar(post("/api/participaciones/iniciar"), ana, "{\"autorizacionId\":" + idAutorizacion + "}", 400);
        enviar(post("/api/sesiones/" + idSesion + "/iniciar"), carlos, null, 200);                     // RF-19
        assertThat(enviar(get("/api/autorizaciones/" + idAutorizacion + "/ingreso"), ana, null, 200)
                .get("permitido").asBoolean()).isTrue();

        // ---------- Módulo 5 (RF-12) + módulo 4 (RF-10, RF-11, RF-23, RF-28): participar ----------
        long idIntento = enviar(post("/api/participaciones/iniciar"), ana,
                "{\"autorizacionId\":" + idAutorizacion + "}", 200).get("id").asLong();
        JsonNode contexto = enviar(get("/api/participaciones/" + idIntento + "/contexto"), ana, null, 200);
        assertThat(contexto.toString()).doesNotContain("esCorrecta").doesNotContain("justificacion");
        assertThat(enviar(get("/api/participaciones/" + idIntento + "/herramientas"), ana, null, 200)).hasSize(1);

        List<long[]> correctas = new ArrayList<>();
        for (JsonNode escenario : contexto.get("escenarios")) {
            for (JsonNode pregunta : escenario.get("preguntas")) {
                long incorrecta = opcion(pregunta, "B");
                enviar(put(respuesta(idIntento, pregunta)), ana, "{\"idRespuesta\":" + incorrecta + "}", 200);
                correctas.add(new long[] {pregunta.get("idPregunta").asLong(), opcion(pregunta, "A")});
            }
        }
        JsonNode progreso = null;
        for (long[] correcta : correctas) {                                                          // RF-10: cambia
            progreso = enviar(put("/api/participaciones/" + idIntento + "/preguntas/" + correcta[0] + "/respuesta"),
                    ana, "{\"idRespuesta\":" + correcta[1] + "}", 200);
        }
        assertThat(progreso.get("porcentajeProgreso").asDouble()).isEqualTo(100.0);                   // RF-11
        assertThat(enviar(get("/api/participaciones/" + idIntento + "/tiempo"), ana, null, 200)
                .get("segundosRestantes").asLong()).isPositive();                                     // RF-23

        // Otro estudiante no puede ver el intento de Ana (Proxy del módulo 4)
        String luis = token(loginEstudiante(LUIS, codigoEnviadoA(LUIS)));
        enviar(get("/api/participaciones/" + idIntento + "/contexto"), luis, null, 403);

        // ---------- Módulo 5 (RF-12, RF-13, RF-14): finalizar y calificar con la rúbrica ----------
        assertThat(enviar(post("/api/participaciones/" + idIntento + "/finalizar"), ana, null, 200)
                .get("estado").asString()).isEqualTo("FINALIZADA");
        JsonNode resultados = enviar(get("/api/resultados"), ana, null, 200);
        assertThat(resultados.get(0).get("notaFinal").asDouble()).isEqualTo(5.0);                    // escala 0-5
        enviar(post("/api/participaciones/iniciar"), ana, "{\"autorizacionId\":" + idAutorizacion + "}", 400);

        // ---------- Módulo 4 (RF-25): repetición excepcional ----------
        assertThat(enviar(post("/api/autorizaciones/" + idAutorizacion + "/repeticiones"), carlos,
                "{\"motivo\":\"Falla de conexión\"}", 201).get("intentosPermitidos").asInt()).isEqualTo(2);
        long idSegundo = enviar(post("/api/participaciones/iniciar"), ana,
                "{\"autorizacionId\":" + idAutorizacion + "}", 200).get("id").asLong();

        // ---------- Módulo 3 (RF-19) → módulo 5 (RF-24): al finalizar la sesión se evalúan los pendientes ----------
        enviar(post("/api/sesiones/" + idSesion + "/finalizar"), carlos, null, 200);
        JsonNode participaciones = enviar(get("/api/participaciones?practica=" + idSesion), carlos, null, 200);
        assertThat(estadoDe(participaciones, idSegundo)).isEqualTo("INCOMPLETA");
        assertThat(estadoDeEstudiante(participaciones, idLuis)).isEqualTo("NO_INICIADA");          // ausencia
        assertThat(loginEstudiante(ANA, codigoAna).getResponse().getStatus())
                .as("el código deja de servir cuando la sesión finaliza").isEqualTo(401);

        // ---------- Módulos 5 y 6 (RF-14, RF-16, RF-26, RF-27) ----------
        enviar(post("/api/resultados/nota-oficial"), carlos, """
                {"practicaId":%d,"estudianteId":%d,"participacionId":%d}""".formatted(idSesion, idAna, idIntento), 200);
        enviar(post("/api/retroalimentaciones"), carlos,
                "{\"participacionId\":" + idIntento + ",\"texto\":\"Buen análisis del fenómeno.\"}", 200);
        enviar(post("/api/resultados/practica/" + idSesion + "/publicar-calificaciones"), carlos, null, 200);
        MvcResult reporte = mvc.perform(get("/api/reportes/evaluacion.xlsx?practica=" + idSesion)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + carlos)).andReturn();
        assertThat(reporte.getResponse().getStatus()).isEqualTo(200);
        assertThat(reporte.getResponse().getContentAsByteArray()).isNotEmpty();

        JsonNode avisos = enviar(get("/api/notificaciones?practica=" + idSesion), ana, null, 200);
        assertThat(avisos.findValuesAsString("tipo"))
                .contains("CONVOCATORIA", "CAMBIO_PRACTICA", "RETROALIMENTACION", "CALIFICACION");
        assertThat(avisos.toString()).as("el código de acceso solo viaja por correo").doesNotContain(codigoAna);

        // Un estudiante no usa funciones de docente (RF-02)
        enviar(get("/api/sesiones"), ana, null, 403);
    }

    // ---------- apoyo ----------

    private JsonNode enviar(MockHttpServletRequestBuilder solicitud, String token, String cuerpo, int estadoEsperado)
            throws Exception {
        solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        MvcResult r = mvc.perform(solicitud).andReturn();
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getResponse().getStatus()).as(r.getRequest().getMethod() + " " + r.getRequest().getRequestURI()
                + " → " + texto).isEqualTo(estadoEsperado);
        return texto.isBlank() ? json.createObjectNode() : json.readTree(texto);
    }

    private String loginPersonal(String correo, String clave) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/personal/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correoInstitucional\":\"" + correo + "\",\"clave\":\"" + clave + "\"}")).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return token(r);
    }

    private MvcResult loginEstudiante(String correo, String codigo) throws Exception {
        return mvc.perform(post("/api/auth/estudiantes/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correoInstitucional\":\"" + correo + "\",\"codigoAutorizacion\":\"" + codigo + "\"}"))
                .andReturn();
    }

    private String token(MvcResult login) throws Exception {
        return json.readTree(login.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asString();
    }

    private long idDocente(String admin, String correo) throws Exception {
        for (JsonNode docente : enviar(get("/api/admin/docentes"), admin, null, 200)) {
            if (docente.get("correoInstitucional").asString().equals(correo)) {
                return docente.get("id").asLong();
            }
        }
        throw new AssertionError("No existe el docente " + correo);
    }

    /** El docente convoca por id de usuario; la API no lista estudiantes, así que se lee de la base. */
    private long idUsuario(String correo) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, correo);
    }

    private String codigoEnviadoA(String destinatario) {
        ArgumentCaptor<SimpleMailMessage> enviados = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo, atLeastOnce()).send(enviados.capture());
        String codigo = null;
        for (SimpleMailMessage mensaje : enviados.getAllValues()) {
            Matcher m = CODIGO.matcher(mensaje.getText());
            if (mensaje.getTo() != null && List.of(mensaje.getTo()).contains(destinatario) && m.find()) {
                codigo = m.group(1);
            }
        }
        assertThat(codigo).as("convocatoria con código para " + destinatario).isNotNull();
        return codigo;
    }

    private static long autorizacionDe(JsonNode convocatorias, long idSesion) {
        for (JsonNode c : convocatorias) {
            if (c.get("idPractica").asLong() == idSesion) {
                return c.get("idAutorizacion").asLong();
            }
        }
        throw new AssertionError("Ana no está convocada a la sesión " + idSesion);
    }

    private static String respuesta(long idIntento, JsonNode pregunta) {
        return "/api/participaciones/" + idIntento + "/preguntas/" + pregunta.get("idPregunta").asLong() + "/respuesta";
    }

    private static long opcion(JsonNode pregunta, String codigo) {
        for (JsonNode o : pregunta.get("opciones")) {
            if (codigo.equals(o.get("codigo").asString())) {
                return o.get("idRespuesta").asLong();
            }
        }
        throw new AssertionError("La pregunta no tiene la opción " + codigo);
    }

    private static String estadoDe(JsonNode participaciones, long id) {
        for (JsonNode p : participaciones) {
            if (p.get("id").asLong() == id) {
                return p.get("estado").asString();
            }
        }
        throw new AssertionError("No está la participación " + id);
    }

    private static String estadoDeEstudiante(JsonNode participaciones, long idEstudiante) {
        for (JsonNode p : participaciones) {
            if (p.get("estudianteId").asLong() == idEstudiante) {
                return p.get("estado").asString();
            }
        }
        throw new AssertionError("No hay participación del estudiante " + idEstudiante);
    }
}
