package co.edu.humboldt.plataformacasos.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContextoSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void recursoProtegidoSinTokenResponde401EnJson() throws Exception {
        mockMvc.perform(get("/api/cualquier-recurso"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso."));
    }

    @Test
    void tokenFalsoResponde401() throws Exception {
        mockMvc.perform(get("/api/cualquier-recurso").header("Authorization", "Bearer token.falso.123"))
                .andExpect(status().isUnauthorized());
    }
}
