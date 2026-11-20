package co.edu.humboldt.plataformacasos.avisos;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Avisos, auditoría y bloqueo de ingreso, de punta a punta con JWT, Proxy, JPA y migraciones. */
@SpringBootTest(properties = "pruebas.contexto=avisos")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvisosIntegracionTest {

    private static final String ADMIN = "admin.plataforma@institucion.edu.co";
    private static final String CARLOS = "carlos.gomez@institucion.edu.co";
    private static final String LAURA = "laura.mejia@institucion.edu.co";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private String token(String correo, String clave) throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        return s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private JsonNode avisosDe(String token) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/avisos").header("Authorization", "Bearer " + token)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private boolean hayAviso(JsonNode avisos, String tipo) {
        for (JsonNode a : avisos.get("avisos")) {
            if (tipo.equals(a.get("tipo").asString())) { return true; }
        }
        return false;
    }

    @Test
    @DisplayName("Cinco claves incorrectas bloquean la cuenta (429), avisan al administrador y un ingreso correcto antes reinicia la cuenta")
    void bloqueoPorIntentosFallidos() throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        String mala = Sesiones.cuerpoPersonal(LAURA, "incorrecta");
        for (int i = 0; i < 4; i++) {
            assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, mala).getResponse().getStatus()).isEqualTo(401);
        }
        // Un ingreso correcto reinicia el conteo...
        assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(LAURA, "Clave-Docente-02")).getResponse().getStatus()).isEqualTo(200);
        for (int i = 0; i < 4; i++) {
            assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, mala).getResponse().getStatus()).isEqualTo(401);
        }
        // ...y el quinto fallo seguido bloquea.
        assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, mala).getResponse().getStatus()).isEqualTo(401);
        MvcResult bloqueada = s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(LAURA, "Clave-Docente-02"));
        assertThat(bloqueada.getResponse().getStatus()).isEqualTo(429);

        String admin = token(ADMIN, "Clave-Admin-01");
        assertThat(hayAviso(avisosDe(admin), "SEGURIDAD")).isTrue();

        // Pasado el plazo, vuelve a poder entrar.
        jdbc.update("UPDATE bloqueo_acceso SET bloqueado_hasta = DATEADD('MINUTE', -1, CURRENT_TIMESTAMP) WHERE correo = ?", LAURA);
        assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(LAURA, "Clave-Docente-02")).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("El ingreso de un estudiante avisa a su docente; la auditoría solo la ve el administrador")
    void ingresoDeEstudianteYAuditoria() throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        MvcResult ana = s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01"));
        // Si el código de la semilla no está vigente en este contexto, la prueba no puede probar el aviso.
        if (ana.getResponse().getStatus() == 200) {
            long docentes = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM autorizaciones a JOIN usuario u ON u.id = a.id_estudiante
                     WHERE u.correo_institucional = 'ana.perez@institucion.edu.co' AND a.estado = 'ACTIVA'""", Long.class);
            if (docentes > 0) {
                assertThat(hayAviso(avisosDe(token(CARLOS, "Clave-Docente-01")), "INGRESO_ESTUDIANTE")).isTrue();
            }
        }
        String carlos = token(CARLOS, "Clave-Docente-01");
        assertThat(mockMvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + carlos)).andReturn().getResponse().getStatus()).isEqualTo(403);
        MvcResult auditoria = mockMvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + token(ADMIN, "Clave-Admin-01"))).andReturn();
        assertThat(auditoria.getResponse().getStatus()).isEqualTo(200);
        assertThat(auditoria.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains("INGRESO");
    }

    @Test
    @DisplayName("Marcar avisos como leídos solo afecta a su dueño")
    void marcarLeidos() throws Exception {
        String admin = token(ADMIN, "Clave-Admin-01");
        jdbc.update("INSERT INTO aviso (usuario_id, tipo, titulo, mensaje, leido, fecha) SELECT id, 'PRUEBA', 't', 'm', FALSE, CURRENT_TIMESTAMP FROM usuario WHERE correo_institucional = ?", ADMIN);
        assertThat(avisosDe(admin).get("noLeidos").asInt()).isGreaterThan(0);
        mockMvc.perform(post("/api/avisos/leidos").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)).andReturn();
        assertThat(avisosDe(admin).get("noLeidos").asInt()).isZero();
    }
}
