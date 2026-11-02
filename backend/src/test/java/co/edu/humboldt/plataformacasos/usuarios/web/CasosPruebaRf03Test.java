package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.soporte.RegistroEvidencias;
import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import co.edu.humboldt.plataformacasos.usuarios.administracion.MensajesAdministracion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;

import static co.edu.humboldt.plataformacasos.soporte.Sesiones.RUTA_LOGIN_PERSONAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Ejecución automatizada (nivel API) de los casos CP-RF03-01 a CP-RF03-06 del documento PT-DCP-01.
 * Cada prueba corre en una transacción que se revierte al final: no altera los datos de otros casos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CasosPruebaRf03Test {

    private static final String DOCENTES = "/api/admin/docentes";
    private static final String ADMIN = Sesiones.cuerpoPersonal("admin.plataforma@institucion.edu.co", "Clave-Admin-01");
    private static final String CARLOS = Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01");
    private static final String LAURA = Sesiones.cuerpoPersonal("laura.mejia@institucion.edu.co", "Clave-Docente-02");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private Sesiones sesiones;

    @BeforeEach
    void preparar() {
        sesiones = new Sesiones(mockMvc, objectMapper);
    }

    private MvcResult enviar(String metodo, String ruta, String token, String cuerpo) throws Exception {
        var solicitud = switch (metodo) {
            case "POST" -> post(ruta);
            case "PUT" -> put(ruta);
            default -> get(ruta);
        };
        if (cuerpo != null && !cuerpo.isEmpty()) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mockMvc.perform(solicitud.header("Authorization", "Bearer " + token)).andReturn();
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long idDocente(MvcResult lista, String correo) throws Exception {
        return StreamSupport.stream(leer(lista).spliterator(), false)
                .filter(d -> d.get("correoInstitucional").asText().equals(correo))
                .findFirst().orElseThrow().get("id").asLong();
    }

    private static int estado(MvcResult r) {
        return r.getResponse().getStatus();
    }

    @Test
    @DisplayName("CP-RF03-01 Crear docente con datos completos")
    void cpRf03_01_crearDocente() throws Exception {
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, ADMIN);
        String token = sesiones.token(login);
        String cuerpo = "{\"nombreCompleto\":\"Marta Ruiz Ocampo\",\"correoInstitucional\":\"nuevo.docente@institucion.edu.co\"}";
        MvcResult creado = enviar("POST", DOCENTES, token, cuerpo);
        MvcResult lista = enviar("GET", DOCENTES, token, "");

        assertThat(estado(creado)).isEqualTo(201);
        assertThat(creado.getResponse().getHeader("Location")).contains(DOCENTES + "/");
        assertThat(leer(creado).get("activo").asBoolean()).isTrue();
        assertThat(leer(creado).get("permisos")).isEmpty();
        assertThat(idDocente(lista, "nuevo.docente@institucion.edu.co")).isPositive();

        RegistroEvidencias.para("CP-RF03-01", "Crear docente con datos completos")
                .paso("POST", RUTA_LOGIN_PERSONAL, ADMIN, login)
                .paso("POST", DOCENTES, cuerpo, creado)
                .paso("GET", DOCENTES, "", lista)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF03-02 Crear docente con datos incompletos")
    void cpRf03_02_datosIncompletos() throws Exception {
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, ADMIN);
        String cuerpo = "{\"nombreCompleto\":\"\",\"correoInstitucional\":\"otro.docente@institucion.edu.co\"}";
        MvcResult r = enviar("POST", DOCENTES, sesiones.token(login), cuerpo);

        assertThat(estado(r)).isEqualTo(400);
        assertThat(leer(r).get("mensaje").asText()).isEqualTo(MensajesAdministracion.NOMBRE_OBLIGATORIO);

        RegistroEvidencias.para("CP-RF03-02", "Crear docente con datos incompletos")
                .paso("POST", RUTA_LOGIN_PERSONAL, ADMIN, login)
                .paso("POST", DOCENTES, cuerpo, r)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF03-03 Crear docente con correo ya registrado")
    void cpRf03_03_correoDuplicado() throws Exception {
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, ADMIN);
        String cuerpo = "{\"nombreCompleto\":\"Carlos Gómez Duplicado\",\"correoInstitucional\":\"carlos.gomez@institucion.edu.co\"}";
        MvcResult r = enviar("POST", DOCENTES, sesiones.token(login), cuerpo);

        assertThat(estado(r)).isEqualTo(409);
        assertThat(leer(r).get("mensaje").asText()).isEqualTo(MensajesAdministracion.CORREO_DUPLICADO);

        RegistroEvidencias.para("CP-RF03-03", "Crear docente con correo ya registrado")
                .paso("POST", RUTA_LOGIN_PERSONAL, ADMIN, login)
                .paso("POST", DOCENTES, cuerpo, r)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF03-04 Asignar permisos a un docente")
    void cpRf03_04_asignarPermisos() throws Exception {
        MvcResult loginAdmin = sesiones.login(RUTA_LOGIN_PERSONAL, ADMIN);
        String tokenAdmin = sesiones.token(loginAdmin);
        MvcResult lista = enviar("GET", DOCENTES, tokenAdmin, "");
        String ruta = DOCENTES + "/" + idDocente(lista, "laura.mejia@institucion.edu.co") + "/permisos";
        String cuerpo = "{\"permisos\":[\"DESCARGAR_REPORTES\"]}";
        MvcResult asignado = enviar("PUT", ruta, tokenAdmin, cuerpo);
        MvcResult loginLaura = sesiones.login(RUTA_LOGIN_PERSONAL, LAURA);
        MvcResult verificacion = enviar("GET", "/api/autorizacion/permisos/DESCARGAR_REPORTES",
                sesiones.token(loginLaura), "");

        assertThat(estado(asignado)).isEqualTo(200);
        assertThat(leer(asignado).get("permisos")).extracting(JsonNode::asText).containsExactly("DESCARGAR_REPORTES");
        assertThat(leer(loginLaura).get("permisos")).extracting(JsonNode::asText).contains("DESCARGAR_REPORTES");
        assertThat(estado(verificacion)).isEqualTo(204);

        RegistroEvidencias.para("CP-RF03-04", "Asignar permisos a un docente")
                .paso("PUT", ruta, cuerpo, asignado)
                .paso("POST", RUTA_LOGIN_PERSONAL, LAURA, loginLaura)
                .paso("GET", "/api/autorizacion/permisos/DESCARGAR_REPORTES", "", verificacion)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF03-05 Retirar un permiso a un docente")
    void cpRf03_05_retirarPermiso() throws Exception {
        String tokenAdmin = sesiones.token(sesiones.login(RUTA_LOGIN_PERSONAL, ADMIN));
        String ruta = DOCENTES + "/" + idDocente(enviar("GET", DOCENTES, tokenAdmin, ""), "carlos.gomez@institucion.edu.co")
                + "/permisos";
        String cuerpo = "{\"permisos\":[]}";
        MvcResult retirado = enviar("PUT", ruta, tokenAdmin, cuerpo);
        MvcResult loginCarlos = sesiones.login(RUTA_LOGIN_PERSONAL, CARLOS);
        MvcResult verificacion = enviar("GET", "/api/autorizacion/permisos/GESTIONAR_CASOS",
                sesiones.token(loginCarlos), "");

        assertThat(estado(retirado)).isEqualTo(200);
        assertThat(leer(retirado).get("permisos")).isEmpty();
        assertThat(estado(verificacion)).isEqualTo(403);

        RegistroEvidencias.para("CP-RF03-05", "Retirar un permiso a un docente")
                .paso("PUT", ruta, cuerpo, retirado)
                .paso("POST", RUTA_LOGIN_PERSONAL, CARLOS, loginCarlos)
                .paso("GET", "/api/autorizacion/permisos/GESTIONAR_CASOS", "", verificacion)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF03-06 Usuario no administrador intenta crear un docente")
    void cpRf03_06_noAdministrador() throws Exception {
        MvcResult login = sesiones.login(RUTA_LOGIN_PERSONAL, CARLOS);
        String token = sesiones.token(login);
        String crear = "{\"nombreCompleto\":\"Intruso\",\"correoInstitucional\":\"intruso@institucion.edu.co\"}";
        String permisos = "{\"permisos\":[\"DESCARGAR_REPORTES\"]}";
        MvcResult r1 = enviar("POST", DOCENTES, token, crear);
        MvcResult r2 = enviar("PUT", DOCENTES + "/3/permisos", token, permisos);
        MvcResult r3 = enviar("GET", DOCENTES, token, "");

        assertThat(estado(r1)).isEqualTo(403);
        assertThat(estado(r2)).isEqualTo(403);
        assertThat(estado(r3)).isEqualTo(403);
        assertThat(leer(r1).get("mensaje").asText()).isEqualTo(AccesoDenegadoException.MENSAJE);

        RegistroEvidencias.para("CP-RF03-06", "Usuario no administrador intenta crear un docente")
                .paso("POST", RUTA_LOGIN_PERSONAL, CARLOS, login)
                .paso("POST", DOCENTES, crear, r1)
                .paso("PUT", DOCENTES + "/3/permisos", permisos, r2)
                .paso("GET", DOCENTES, "", r3)
                .guardar();
    }
}
