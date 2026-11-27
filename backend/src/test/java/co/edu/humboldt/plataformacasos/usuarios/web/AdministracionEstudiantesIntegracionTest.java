package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registro de estudiantes de punta a punta (JWT, Proxy de acceso, JPA, migraciones y semilla ficticia):
 * el administrador los crea y quedan disponibles para que el docente los convoque (RF-04).
 */
@SpringBootTest(properties = "pruebas.contexto=administracion-estudiantes")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministracionEstudiantesIntegracionTest {

    private static final String ESTUDIANTES = "/api/admin/estudiantes";
    private static final String ADMIN = "admin.plataforma@institucion.edu.co";
    private static final String CARLOS = "carlos.gomez@institucion.edu.co";
    private static final String NUEVO = "nuevo.estudiante@institucion.edu.co";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private record Respuesta(int estado, JsonNode cuerpo) {
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM usuario WHERE correo_institucional = ?", NUEVO);
        jdbc.update("DELETE FROM usuario_permiso WHERE permiso = 'PROGRAMAR_SESIONES' AND usuario_id = ?", id(CARLOS));
    }

    private long id(String correo) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, correo);
    }

    private String token(String correo, String clave) throws Exception {
        Sesiones sesiones = new Sesiones(mockMvc, json);
        return sesiones.token(sesiones.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private String admin() throws Exception {
        return token(ADMIN, "Clave-Admin-01");
    }

    private Respuesta enviar(HttpMethod metodo, String ruta, String token, String cuerpo) throws Exception {
        MockHttpServletRequestBuilder solicitud = MockMvcRequestBuilders.request(metodo, ruta);
        if (token != null) {
            solicitud.header("Authorization", "Bearer " + token);
        }
        if (cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        MvcResult r = mockMvc.perform(solicitud).andReturn();
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(r.getResponse().getStatus(), texto.isEmpty() ? null : json.readTree(texto));
    }

    private static String cuerpo(String nombre, String correo) {
        return "{\"nombreCompleto\":\"" + nombre + "\",\"correoInstitucional\":\"" + correo + "\"}";
    }

    private static List<String> correos(Respuesta listado) {
        List<String> correos = new ArrayList<>();
        listado.cuerpo().forEach(u -> correos.add(u.get("correoInstitucional").asString()));
        return correos;
    }

    @Test
    @DisplayName("El administrador registra un estudiante y el docente lo encuentra para convocarlo")
    void registraYQuedaDisponibleParaConvocar() throws Exception {
        String admin = admin();

        Respuesta creado = enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo("Nuevo Estudiante", " Nuevo.Estudiante@Institucion.edu.co "));
        assertThat(creado.estado()).isEqualTo(201);
        assertThat(creado.cuerpo().get("correoInstitucional").asString()).isEqualTo(NUEVO);
        assertThat(creado.cuerpo().get("activo").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT rol FROM usuario WHERE correo_institucional = ?", String.class, NUEVO))
                .isEqualTo("ESTUDIANTE");

        assertThat(correos(enviar(HttpMethod.GET, ESTUDIANTES, admin, null))).contains(NUEVO, "ana.perez@institucion.edu.co");

        // RF-04: el docente que programa sesiones lo ve en la lista para convocar.
        jdbc.update("INSERT INTO usuario_permiso (usuario_id, permiso) VALUES (?, 'PROGRAMAR_SESIONES')", id(CARLOS));
        Respuesta paraConvocar = enviar(HttpMethod.GET, "/api/estudiantes", token(CARLOS, "Clave-Docente-01"), null);
        assertThat(paraConvocar.estado()).isEqualTo(200);
        assertThat(correos(paraConvocar)).contains(NUEVO);
    }

    @Test
    @DisplayName("Solo el administrador registra estudiantes: un docente recibe 403 y sin sesión 401")
    void soloElAdministrador() throws Exception {
        String carlos = token(CARLOS, "Clave-Docente-01");

        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, carlos, cuerpo("Nuevo", NUEVO)).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.GET, ESTUDIANTES, carlos, null).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, null, cuerpo("Nuevo", NUEVO)).estado()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo_institucional = ?", Integer.class, NUEVO))
                .isZero();
    }

    @Test
    @DisplayName("Rechaza datos inválidos, otro dominio y correos ya registrados")
    void validaciones() throws Exception {
        String admin = admin();

        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo(" ", NUEVO)).estado()).isEqualTo(400);
        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo("Nuevo", "sin-arroba")).estado()).isEqualTo(400);
        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo("Nuevo", "nuevo@gmail.com")).estado()).isEqualTo(400);
        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo("Ana", "ana.perez@institucion.edu.co")).estado())
                .isEqualTo(409);
        assertThat(enviar(HttpMethod.POST, ESTUDIANTES, admin, cuerpo("Carlos", CARLOS)).estado()).isEqualTo(409);
    }
}
