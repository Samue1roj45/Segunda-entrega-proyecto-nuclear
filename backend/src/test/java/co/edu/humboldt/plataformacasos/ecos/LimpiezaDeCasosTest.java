package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Corre docs/limpiar-datos-de-casos.sql sobre la base de ejemplo: borra los casos, conserva usuarios y Ecos. */
@SpringBootTest(properties = "pruebas.contexto=limpieza")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LimpiezaDeCasosTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource datos;

    private int cuantos(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    @Test
    @DisplayName("La limpieza borra los datos de casos y deja usuarios; Ana y Luis entran con su contraseña")
    void limpia() throws Exception {
        // Algo de Ecos que debe sobrevivir.
        jdbc.update("INSERT INTO ecos_caso (caso_version, caso_id, nombre, credito_parcial, cargado_en) VALUES ('0123456789abcdef0123456789abcdef','C','Caso',TRUE,CURRENT_TIMESTAMP)");
        assertThat(cuantos("casos_estudio")).isPositive();
        assertThat(cuantos("autorizaciones")).isPositive();
        int usuarios = cuantos("usuario");

        for (int vez = 0; vez < 2; vez++) {   // repetible
            try (var c = datos.getConnection()) {
                ScriptUtils.executeSqlScript(c, new FileSystemResource(Path.of("..", "docs", "limpiar-datos-de-casos.sql")));
            }
        }

        for (String t : new String[] {"casos_estudio", "escenarios", "preguntas", "respuestas", "rubricas", "criterios_evaluacion",
                "practicas", "autorizaciones", "participacion", "resultado", "aviso"}) {
            assertThat(cuantos(t)).as(t).isZero();
        }
        assertThat(cuantos("usuario")).isEqualTo(usuarios);
        assertThat(cuantos("ecos_caso")).isEqualTo(1);
        assertThat(cuantos("docente_estudiante")).isEqualTo(2);

        Sesiones s = new Sesiones(mockMvc, json);
        assertThat(s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("ana.perez@institucion.edu.co", "Clave-Estudiante-01"))
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(s.login(Sesiones.RUTA_LOGIN_ESTUDIANTE, Sesiones.cuerpoEstudiante("luis.rios@institucion.edu.co", "Clave-Estudiante-02"))
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal("carlos.gomez@institucion.edu.co", "Clave-Docente-01"))
                .getResponse().getStatus()).isEqualTo(200);
    }
}
