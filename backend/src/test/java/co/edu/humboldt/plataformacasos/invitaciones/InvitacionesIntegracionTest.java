package co.edu.humboldt.plataformacasos.invitaciones;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Invitación por correo → cuenta del estudiante con su propia contraseña → vínculo con el docente. */
@SpringBootTest(properties = "pruebas.contexto=invitaciones")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvitacionesIntegracionTest {

    private static final String NUEVA = "sofia.nueva@institucion.edu.co";
    private static final String CLAVE = "Ecos-2026-Fuerte";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private record Resp(int estado, JsonNode cuerpo) {
        String codigo() { return cuerpo.has("codigo") ? cuerpo.get("codigo").asString() : null; }
    }

    private Resp llamar(MockHttpServletRequestBuilder req, String token, Object cuerpo) throws Exception {
        req = req.contentType(MediaType.APPLICATION_JSON);
        if (cuerpo != null) { req = req.content(json.writeValueAsString(cuerpo)); }
        if (token != null) { req = req.header("Authorization", "Bearer " + token); }
        MvcResult r = mockMvc.perform(req).andReturn();
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Resp(r.getResponse().getStatus(), texto.isBlank() ? json.createObjectNode() : json.readTree(texto));
    }

    private String carlos() throws Exception {
        jdbc.update("INSERT INTO usuario_permiso (usuario_id, permiso) SELECT id, 'INVITAR_ESTUDIANTES' FROM usuario WHERE correo_institucional = 'carlos.gomez@institucion.edu.co' AND NOT EXISTS (SELECT 1 FROM usuario_permiso up WHERE up.usuario_id = usuario.id AND up.permiso = 'INVITAR_ESTUDIANTES')");
        Sesiones s = new Sesiones(mockMvc, json);
        return s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01")));
    }

    private int loginEstudiante(String correo, String clave) throws Exception {
        return llamar(post("/api/auth/estudiantes/login"), null, Map.of("correoInstitucional", correo, "codigoAutorizacion", clave)).estado();
    }

    private static String tokenDe(JsonNode resultado) {
        String enlace = resultado.get("enlace").asString();
        return enlace.substring(enlace.lastIndexOf('/') + 1);
    }

    private Map<String, Object> datos(String documento) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("nombreCompleto", "Sofía Nueva Torres"); d.put("tipoDocumento", "CC"); d.put("numeroDocumento", documento);
        d.put("fechaNacimiento", "2003-05-17"); d.put("telefono", "3001234567"); d.put("contrasena", CLAVE); d.put("aceptaTratamientoDatos", true);
        return d;
    }

    @Test
    @DisplayName("El docente invita, el estudiante crea su cuenta desde el enlace y queda en la lista del docente")
    void flujoDeInvitacion() throws Exception {
        String carlos = carlos();
        // Sin permiso no se puede invitar.
        Sesiones s = new Sesiones(mockMvc, json);
        String laura = s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal("laura.mejia@institucion.edu.co", "Clave-Docente-02")));
        assertThat(llamar(post("/api/invitaciones"), laura, Map.of("correos", List.of(NUEVA))).estado()).isEqualTo(403);

        Resp r = llamar(post("/api/invitaciones"), carlos, Map.of("correos", List.of(NUEVA, "otro@gmail.com", "mal-correo",
                "laura.mejia@institucion.edu.co", "ana.perez@institucion.edu.co")));
        assertThat(r.estado()).isEqualTo(200);
        assertThat(r.cuerpo().get(0).get("resultado").asString()).isEqualTo("ENVIADA");
        assertThat(r.cuerpo().get(0).get("correoEnviado").asBoolean()).isFalse();           // en pruebas no hay servidor de correo...
        assertThat(r.cuerpo().get(0).get("enlace").asString()).contains("/registro/");       // ...y el docente recibe el enlace para compartirlo
        assertThat(r.cuerpo().get(1).get("resultado").asString()).isEqualTo("DOMINIO_INVALIDO");
        assertThat(r.cuerpo().get(2).get("resultado").asString()).isEqualTo("CORREO_INVALIDO");
        assertThat(r.cuerpo().get(3).get("resultado").asString()).isEqualTo("NO_ES_ESTUDIANTE");
        assertThat(r.cuerpo().get(4).get("resultado").asString()).isEqualTo("VINCULADO");   // Ana ya tenía cuenta: solo se vincula
        String token = tokenDe(r.cuerpo().get(0));
        // El token no queda en claro en la base.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM invitacion_estudiante WHERE token_hash = ?", Integer.class, token)).isZero();

        // Quien abre el enlace ve el correo (que no se escribe) y quién lo invitó, sin sesión.
        Resp info = llamar(get("/api/invitaciones/publica/" + token), null, null);
        assertThat(info.estado()).isEqualTo(200);
        assertThat(info.cuerpo().get("correo").asString()).isEqualTo(NUEVA);
        assertThat(info.cuerpo().get("docente").asString()).contains("Carlos");
        assertThat(llamar(get("/api/invitaciones/publica/token-inventado"), null, null).codigo()).isEqualTo("INVITACION_NO_EXISTE");

        // Validaciones del registro.
        String ruta = "/api/invitaciones/publica/" + token + "/aceptar";
        Map<String, Object> debil = datos("1012345678"); debil.put("contrasena", "corta");
        assertThat(llamar(post(ruta), null, debil).estado()).isEqualTo(422);
        Map<String, Object> sinAceptar = datos("1012345678"); sinAceptar.put("aceptaTratamientoDatos", false);
        assertThat(llamar(post(ruta), null, sinAceptar).estado()).isEqualTo(422);
        Map<String, Object> menor = datos("1012345678"); menor.put("fechaNacimiento", "2020-01-01");
        assertThat(llamar(post(ruta), null, menor).estado()).isEqualTo(422);
        Map<String, Object> unNombre = datos("1012345678"); unNombre.put("nombreCompleto", "Sofía");
        assertThat(llamar(post(ruta), null, unNombre).estado()).isEqualTo(422);
        Map<String, Object> conCorreo = datos("1012345678"); conCorreo.put("contrasena", "Sofia.nueva-2026");
        assertThat(llamar(post(ruta), null, conCorreo).estado()).isEqualTo(422);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo_institucional = ?", Integer.class, NUEVA)).isZero();

        // Registro correcto: 201, la cuenta existe, está vinculada al docente y la invitación queda usada.
        assertThat(llamar(post(ruta), null, datos("1012345678")).estado()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM estudiante_perfil WHERE numero_documento = '1012345678'", Integer.class)).isEqualTo(1);
        assertThat(llamar(post(ruta), null, datos("1012345678")).codigo()).isEqualTo("INVITACION_USADA");
        assertThat(llamar(get("/api/invitaciones/publica/" + token), null, null).estado()).isEqualTo(410);

        // La contraseña es suya (distingue mayúsculas) y sirve para ingresar.
        assertThat(loginEstudiante(NUEVA, CLAVE)).isEqualTo(200);
        assertThat(loginEstudiante(NUEVA, CLAVE.toLowerCase())).isEqualTo(401);
        assertThat(loginEstudiante(NUEVA, "Otra-Clave-2026")).isEqualTo(401);

        // Está en "mis estudiantes" de Carlos; las invitaciones muestran quién la aceptó.
        Resp lista = llamar(get("/api/ecos/v1/mis-estudiantes"), carlos, null);
        assertThat(lista.cuerpo().toString()).contains(NUEVA).contains("SIN_JUGAR");
        assertThat(llamar(get("/api/invitaciones"), carlos, null).cuerpo().toString()).contains("ACEPTADA").contains("Sofía Nueva Torres");

        // Un documento repetido no deja crear otra cuenta.
        Resp otra = llamar(post("/api/invitaciones"), carlos, Map.of("correos", List.of("camila.otra@institucion.edu.co")));
        Resp repetido = llamar(post("/api/invitaciones/publica/" + tokenDe(otra.cuerpo().get(0)) + "/aceptar"), null, datos("1012345678"));
        assertThat(repetido.codigo()).isEqualTo("DOCUMENTO_YA_REGISTRADO");

        // Retirar a la estudiante de la lista del docente NO la saca de la plataforma.
        long id = lista.cuerpo().get(0).get("id").asLong();
        assertThat(mockMvc.perform(delete("/api/ecos/v1/mis-estudiantes/" + id).header("Authorization", "Bearer " + carlos)).andReturn()
                .getResponse().getStatus()).isEqualTo(204);
        assertThat(loginEstudiante(NUEVA, CLAVE)).isEqualTo(200);
    }

    @Test
    @DisplayName("Reenviar cambia el enlace, cancelar lo inutiliza y una invitación vencida no sirve")
    void reenviarCancelarYVencer() throws Exception {
        String carlos = carlos();
        Resp primera = llamar(post("/api/invitaciones"), carlos, Map.of("correos", List.of("pedro.pendiente@institucion.edu.co")));
        String viejo = tokenDe(primera.cuerpo().get(0));
        long id = jdbc.queryForObject("SELECT id FROM invitacion_estudiante WHERE correo = 'pedro.pendiente@institucion.edu.co'", Long.class);

        Resp reenviada = llamar(post("/api/invitaciones/" + id + "/reenviar"), carlos, null);
        String nuevo = tokenDe(reenviada.cuerpo());
        assertThat(nuevo).isNotEqualTo(viejo);
        assertThat(llamar(get("/api/invitaciones/publica/" + viejo), null, null).estado()).isEqualTo(404);
        assertThat(llamar(get("/api/invitaciones/publica/" + nuevo), null, null).estado()).isEqualTo(200);
        // Invitar de nuevo al mismo correo no duplica: reenvía.
        assertThat(llamar(post("/api/invitaciones"), carlos, Map.of("correos", List.of("pedro.pendiente@institucion.edu.co")))
                .cuerpo().get(0).get("resultado").asString()).isEqualTo("REENVIADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM invitacion_estudiante WHERE correo = 'pedro.pendiente@institucion.edu.co'", Integer.class)).isEqualTo(1);

        // Vencida.
        String vigente = tokenDe(llamar(post("/api/invitaciones/" + id + "/reenviar"), carlos, null).cuerpo());
        jdbc.update("UPDATE invitacion_estudiante SET vence_en = DATEADD('DAY', -1, CURRENT_TIMESTAMP) WHERE id = ?", id);
        assertThat(llamar(get("/api/invitaciones/publica/" + vigente), null, null).codigo()).isEqualTo("INVITACION_VENCIDA");
        assertThat(llamar(get("/api/invitaciones"), carlos, null).cuerpo().toString()).contains("VENCIDA");

        // Cancelada.
        String otro = tokenDe(llamar(post("/api/invitaciones/" + id + "/reenviar"), carlos, null).cuerpo());
        assertThat(mockMvc.perform(delete("/api/invitaciones/" + id).header("Authorization", "Bearer " + carlos)).andReturn()
                .getResponse().getStatus()).isEqualTo(204);
        assertThat(llamar(get("/api/invitaciones/publica/" + otro), null, null).codigo()).isEqualTo("INVITACION_CANCELADA");
    }
}
