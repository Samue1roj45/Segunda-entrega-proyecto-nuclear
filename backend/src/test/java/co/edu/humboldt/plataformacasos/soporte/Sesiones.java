package co.edu.humboldt.plataformacasos.soporte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Inicia sesión con los usuarios ficticios del perfil de pruebas y devuelve la respuesta del login. */
public final class Sesiones {

    public static final String RUTA_LOGIN_ESTUDIANTE = "/api/auth/estudiantes/login";
    public static final String RUTA_LOGIN_PERSONAL = "/api/auth/personal/login";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public Sesiones(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public static String cuerpoEstudiante(String correo, String codigo) {
        return "{\"correoInstitucional\":\"" + correo + "\",\"codigoAutorizacion\":\"" + codigo + "\"}";
    }

    public static String cuerpoPersonal(String correo, String clave) {
        return "{\"correoInstitucional\":\"" + correo + "\",\"clave\":\"" + clave + "\"}";
    }

    public MvcResult login(String ruta, String cuerpo) throws Exception {
        return mockMvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    public String token(MvcResult login) throws Exception {
        return objectMapper.readTree(login.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("token").asText();
    }
}
