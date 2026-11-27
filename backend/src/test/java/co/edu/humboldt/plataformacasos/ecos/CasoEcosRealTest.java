package co.edu.humboldt.plataformacasos.ecos;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** El banco real de Ecos queda cargado al arrancar y se convierte en un caso publicado del docente. */
@SpringBootTest(properties = {"pruebas.contexto=ecos-real", "app.ecos.catalogo-inicial=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CasoEcosRealTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("El catálogo real suma 100 puntos y su caso queda publicado con 14 preguntas y 3 escenarios")
    void casoReal() throws Exception {
        assertThat(jdbc.queryForObject("SELECT SUM(puntos) FROM ecos_pregunta", Integer.class)).isEqualTo(100);
        Sesiones s = new Sesiones(mockMvc, json);
        String carlos = s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01")));
        var r = mockMvc.perform(post("/api/ecos/v1/crear-caso").header("Authorization", "Bearer " + carlos)).andReturn().getResponse();
        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(201);
        Long id = jdbc.queryForObject("SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión'", Long.class);
        assertThat(jdbc.queryForObject("SELECT estado FROM casos_estudio WHERE id_caso = ?", String.class, id)).isEqualTo("PUBLICADO");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM escenarios WHERE id_caso = ?", Integer.class, id)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM preguntas p JOIN escenarios e ON e.id_escenario = p.id_escenario WHERE e.id_caso = ?", Integer.class, id)).isEqualTo(14);
    }
}
