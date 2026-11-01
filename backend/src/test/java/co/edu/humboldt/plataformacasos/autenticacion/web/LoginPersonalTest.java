package co.edu.humboldt.plataformacasos.autenticacion.web;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inicio de sesión del personal: base para ejecutar los casos de RF-02 y RF-03. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginPersonalTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions login(String correo, String clave) throws Exception {
        String cuerpo = "{\"correoInstitucional\":\"" + correo + "\",\"clave\":\"" + clave + "\"}";
        return mockMvc.perform(post("/api/auth/personal/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Test
    void administradorIniciaSesionConSusPermisosDeRol() throws Exception {
        login("admin.plataforma@institucion.edu.co", "Clave-Admin-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.permisos", contains("ADMINISTRAR_DOCENTES")));
    }

    @Test
    void docenteIniciaSesionConLosPermisosAsignados() throws Exception {
        login("carlos.gomez@institucion.edu.co", "Clave-Docente-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("DOCENTE"))
                .andExpect(jsonPath("$.permisos", contains("GESTIONAR_CASOS")));
    }

    @Test
    void contrasenaIncorrectaResponde401Generico() throws Exception {
        login("carlos.gomez@institucion.edu.co", "otra-clave")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value(MensajesAutenticacion.CREDENCIALES_PERSONAL_INVALIDAS));
    }

    @Test
    void unEstudianteNoPuedeUsarElIngresoDelPersonal() throws Exception {
        login("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void camposVaciosResponden400() throws Exception {
        login("", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MensajesAutenticacion.CORREO_Y_CLAVE_OBLIGATORIOS));
    }
}
