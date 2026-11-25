package co.edu.humboldt.plataformacasos.sesiones;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Retirar a un estudiante de una sesión lo saca de ESA sesión, no de la plataforma: sigue pudiendo iniciar sesión. */
@SpringBootTest(properties = "pruebas.contexto=retiro")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RetiroNoSacaDeLaPlataformaTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("El código es la contraseña: sirve aunque lo retiren de la sesión o la sesión finalice")
    void retiradoPuedeEntrar() throws Exception {
        long convocados = jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones", Long.class);
        if (convocados == 0) {
            org.junit.jupiter.api.Assumptions.abort("este contexto no trae la sesión de ejemplo");
        }
        jdbc.update("UPDATE autorizaciones SET estado = 'RETIRADA'");
        Sesiones s = new Sesiones(mockMvc, json);
        var login = s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("luis.rios@institucion.edu.co", "CODIGO_VALIDO_02"));
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        // Con un código equivocado sigue sin entrar.
        var malo = s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("luis.rios@institucion.edu.co", "CODIGO_FALSO_99"));
        assertThat(malo.getResponse().getStatus()).isEqualTo(401);
        // Tampoco depende de que la sesión siga abierta.
        jdbc.update("UPDATE practicas SET estado_practica = 'FINALIZADO'");
        var finalizada = s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("luis.rios@institucion.edu.co", "CODIGO_VALIDO_02"));
        assertThat(finalizada.getResponse().getStatus()).isEqualTo(200);
    }
}
