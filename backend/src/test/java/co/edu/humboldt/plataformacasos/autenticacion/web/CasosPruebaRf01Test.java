package co.edu.humboldt.plataformacasos.autenticacion.web;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.soporte.RegistroEvidencias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Ejecución automatizada (nivel API) de los casos CP-RF01-01 a CP-RF01-06 del documento PT-DCP-01,
 * con los datos ficticios del perfil de pruebas. Cada caso guarda su evidencia en target/evidencias.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CasosPruebaRf01Test {

    private static final String RUTA = "/api/auth/estudiantes/login";
    private static final String ANA = "ana.perez@institucion.edu.co";
    private static final String LUIS = "luis.rios@institucion.edu.co";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtDecoder jwtDecoder;

    private MvcResult login(String cuerpo) throws Exception {
        return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private static String cuerpo(String correo, String codigo) {
        return "{\"correoInstitucional\":" + json(correo) + ",\"codigoAutorizacion\":" + json(codigo) + "}";
    }

    private static String json(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("CP-RF01-01 Autenticación correcta")
    void cpRf01_01_autenticacionCorrecta() throws Exception {
        String solicitud = cuerpo(ANA, "CODIGO_VALIDO_01");
        MvcResult r = login(solicitud);
        JsonNode cuerpo = leer(r);

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo.get("rol").asText()).isEqualTo("ESTUDIANTE");
        assertThat(cuerpo.get("tipoToken").asText()).isEqualTo("Bearer");
        Jwt token = jwtDecoder.decode(cuerpo.get("token").asText());
        assertThat(token.getClaimAsString(EmisorToken.RECLAMO_CORREO)).isEqualTo(ANA);
        assertThat(token.getClaimAsStringList(EmisorToken.RECLAMO_PERMISOS))
                .containsExactlyInAnyOrder("PARTICIPAR_EN_CASOS", "CONSULTAR_RESULTADOS_PROPIOS");

        RegistroEvidencias.para("CP-RF01-01", "Autenticación correcta").paso("POST", RUTA, solicitud, r).guardar();
    }

    @Test
    @DisplayName("CP-RF01-02 Código de autorización inválido")
    void cpRf01_02_codigoInvalido() throws Exception {
        String solicitud = cuerpo(ANA, "CODIGO_INVALIDO_99");
        MvcResult r = login(solicitud);

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(leer(r).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
        assertThat(leer(r).has("token")).isFalse();

        RegistroEvidencias.para("CP-RF01-02", "Código de autorización inválido").paso("POST", RUTA, solicitud, r).guardar();
    }

    @Test
    @DisplayName("CP-RF01-03 Correo institucional no registrado")
    void cpRf01_03_correoNoRegistrado() throws Exception {
        String solicitud = cuerpo("no.registrado@institucion.edu.co", "CODIGO_VALIDO_01");
        MvcResult r = login(solicitud);

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(leer(r).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);

        RegistroEvidencias.para("CP-RF01-03", "Correo institucional no registrado").paso("POST", RUTA, solicitud, r).guardar();
    }

    @Test
    @DisplayName("CP-RF01-04 Correo que no es institucional")
    void cpRf01_04_correoNoInstitucional() throws Exception {
        String solicitud = cuerpo("ana.perez@gmail.com", "CODIGO_VALIDO_01");
        MvcResult r = login(solicitud);

        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r).get("mensaje").asText())
                .isEqualTo(MensajesAutenticacion.correoNoInstitucional("institucion.edu.co"));

        RegistroEvidencias.para("CP-RF01-04", "Correo que no es institucional").paso("POST", RUTA, solicitud, r).guardar();
    }

    @Test
    @DisplayName("CP-RF01-05 Campos obligatorios vacíos")
    void cpRf01_05_camposVacios() throws Exception {
        String sinCorreo = cuerpo("", "CODIGO_VALIDO_01");
        String sinCodigo = cuerpo(ANA, "");
        String sinNada = "{}";
        MvcResult r1 = login(sinCorreo);
        MvcResult r2 = login(sinCodigo);
        MvcResult r3 = login(sinNada);

        assertThat(r1.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r1).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CORREO_OBLIGATORIO);
        assertThat(r2.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r2).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CODIGO_OBLIGATORIO);
        assertThat(r3.getResponse().getStatus()).isEqualTo(400);
        assertThat(leer(r3).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CAMPOS_OBLIGATORIOS);

        RegistroEvidencias.para("CP-RF01-05", "Campos obligatorios vacíos")
                .paso("POST", RUTA, sinCorreo, r1)
                .paso("POST", RUTA, sinCodigo, r2)
                .paso("POST", RUTA, sinNada, r3)
                .guardar();
    }

    @Test
    @DisplayName("CP-RF01-06 Código asociado a otro estudiante")
    void cpRf01_06_codigoDeOtroEstudiante() throws Exception {
        String solicitud = cuerpo(LUIS, "CODIGO_VALIDO_01");
        MvcResult r = login(solicitud);

        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(leer(r).get("mensaje").asText()).isEqualTo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);

        RegistroEvidencias.para("CP-RF01-06", "Código asociado a otro estudiante").paso("POST", RUTA, solicitud, r).guardar();
    }
}
