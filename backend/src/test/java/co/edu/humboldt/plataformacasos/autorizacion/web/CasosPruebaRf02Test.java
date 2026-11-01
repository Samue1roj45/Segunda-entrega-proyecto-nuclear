package co.edu.humboldt.plataformacasos.autorizacion.web;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.NoAutenticadoException;
import co.edu.humboldt.plataformacasos.soporte.RegistroEvidencias;
import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static co.edu.humboldt.plataformacasos.soporte.Sesiones.RUTA_LOGIN_ESTUDIANTE;
import static co.edu.humboldt.plataformacasos.soporte.Sesiones.RUTA_LOGIN_PERSONAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Ejecución automatizada (nivel API) de los casos CP-RF02-01 a CP-RF02-06 del documento PT-DCP-01.
 * La verificación ocurre en el servidor, aunque se omita cualquier interfaz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CasosPruebaRf02Test {

    private static final String SESION = "/api/sesion";
    private static final String PERMISO = "/api/autorizacion/permisos/";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private Sesiones sesiones;

    @BeforeEach
    void preparar() {
        sesiones = new Sesiones(mockMvc, objectMapper);
    }

    private MvcResult getCon(String token, String ruta) throws Exception {
        return token == null
                ? mockMvc.perform(get(ruta)).andReturn()
                : mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token)).andReturn();
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static int estado(MvcResult r) {
        return r.getResponse().getStatus();
    }

    @Test
    @DisplayName("CP-RF02-01 Estudiante accede a sus funciones")
    void cpRf02_01_estudianteAccedeASusFunciones() throws Exception {
        String cuerpo = Sesiones.cuerpoEstudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01");
        MvcResult login = sesiones.login(RUTA_LOGIN_ESTUDIANTE, cuerpo);
        String token = sesiones.token(login);
        MvcResult sesion = getCon(token, SESION);
        MvcResult participar = getCon(token, PERMISO + "PARTICIPAR_EN_CASOS");

        assertThat(estado(sesion)).isEqualTo(200);
        assertThat(leer(sesion).get("rol").asText()).isEqualTo("ESTUDIANTE");
        assertThat(leer(sesion).get("permisos")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("PARTICIPAR_EN_CASOS", "CONSULTAR_RESULTADOS_PROPIOS");
        assertThat(estado(participar)).isEqualTo(204);

        RegistroEvidencias.para("CP-RF02-01", "Estudiante accede a sus funciones")
                .paso("POST", RUTA_LOGIN_ESTUDIANTE, cuerpo, login)
                .paso("GET", SESION, "", sesion)
                .paso("GET", PERMISO + "PARTICIPAR_EN_CASOS", "", participar)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF02-02 Estudiante intenta usar una función docente")
    void cpRf02_02_estudianteIntentaFuncionDocente() throws Exception {
        String cuerpo = Sesiones.cuerpoEstudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01");
        MvcResult login = sesiones.login(RUTA_LOGIN_ESTUDIANTE, cuerpo);
        MvcResult gestionar = getCon(sesiones.token(login), PERMISO + "GESTIONAR_CASOS");

        assertThat(estado(gestionar)).isEqualTo(403);
        assertThat(leer(gestionar).get("mensaje").asText()).isEqualTo(AccesoDenegadoException.MENSAJE);

        RegistroEvidencias.para("CP-RF02-02", "Estudiante intenta usar una función docente")
                .paso("POST", RUTA_LOGIN_ESTUDIANTE, cuerpo, login)
                .paso("GET", PERMISO + "GESTIONAR_CASOS", "", gestionar)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF02-03 Docente con permiso accede a la función")
    void cpRf02_03_docenteConPermiso() throws Exception {
        String cuerpo = Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01");
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, cuerpo);
        MvcResult gestionar = getCon(sesiones.token(login), PERMISO + "GESTIONAR_CASOS");

        assertThat(estado(gestionar)).isEqualTo(204);

        RegistroEvidencias.para("CP-RF02-03", "Docente con permiso accede a la función")
                .paso("POST", RUTA_LOGIN_PERSONAL, cuerpo, login)
                .paso("GET", PERMISO + "GESTIONAR_CASOS", "", gestionar)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF02-04 Docente sin el permiso requerido")
    void cpRf02_04_docenteSinPermiso() throws Exception {
        String cuerpo = Sesiones.cuerpoPersonal("laura.mejia@institucion.edu.co", "Clave-Docente-02");
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, cuerpo);
        String token = sesiones.token(login);
        MvcResult sesion = getCon(token, SESION);
        MvcResult reportes = getCon(token, PERMISO + "DESCARGAR_REPORTES");

        assertThat(leer(sesion).get("permisos")).isEmpty();
        assertThat(estado(reportes)).isEqualTo(403);

        RegistroEvidencias.para("CP-RF02-04", "Docente sin el permiso requerido")
                .paso("POST", RUTA_LOGIN_PERSONAL, cuerpo, login)
                .paso("GET", SESION, "", sesion)
                .paso("GET", PERMISO + "DESCARGAR_REPORTES", "", reportes)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF02-05 Docente intenta usar una función de administrador")
    void cpRf02_05_docenteIntentaFuncionDeAdministrador() throws Exception {
        String cuerpo = Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01");
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, cuerpo);
        MvcResult administrar = getCon(sesiones.token(login), PERMISO + "ADMINISTRAR_DOCENTES");

        assertThat(estado(administrar)).isEqualTo(403);

        RegistroEvidencias.para("CP-RF02-05", "Docente intenta usar una función de administrador")
                .paso("POST", RUTA_LOGIN_PERSONAL, cuerpo, login)
                .paso("GET", PERMISO + "ADMINISTRAR_DOCENTES", "", administrar)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF02-06 Acceso sin sesión iniciada")
    void cpRf02_06_accesoSinSesion() throws Exception {
        MvcResult sesion = getCon(null, SESION);
        MvcResult participar = getCon(null, PERMISO + "PARTICIPAR_EN_CASOS");

        assertThat(estado(sesion)).isEqualTo(401);
        assertThat(estado(participar)).isEqualTo(401);
        assertThat(leer(sesion).get("mensaje").asText()).isEqualTo(NoAutenticadoException.MENSAJE);

        RegistroEvidencias.para("CP-RF02-06", "Acceso sin sesión iniciada")
                .paso("GET", SESION, "", sesion)
                .paso("GET", PERMISO + "PARTICIPAR_EN_CASOS", "", participar)
                .guardar();
    }
}
