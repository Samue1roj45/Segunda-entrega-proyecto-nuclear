package com.unihumboldt.nuclear.evaluacion.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAutenticacionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder encoder;

    @Test
    void tokenFirmadoListaParticipaciones() throws Exception {
        mockMvc.perform(get("/api/participaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(claimsEstudiante(), Instant.now().plusSeconds(600))))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get("/api/participaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerInvalidoEs401() throws Exception {
        mockMvc.perform(get("/api/participaciones")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoEs401() throws Exception {
        mockMvc.perform(get("/api/participaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(claimsEstudiante(), Instant.now().minusSeconds(180))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabecerasSolasNoAutentican() throws Exception {
        mockMvc.perform(get("/api/participaciones")
                        .header("X-Rol", "ADMIN")
                        .header("X-Usuario-Id", "1")
                        .header("X-Estudiante-Id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDePruebaConCabecerasNoTieneRol() throws Exception {
        mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est10"))
                        .header("X-Rol", "ESTUDIANTE")
                        .header("X-Usuario-Id", "10")
                        .header("X-Estudiante-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenDeDocenteIgnoraCabecerasDeEstudiante() throws Exception {
        Map<String, Object> claims = Map.of(
                "rol", "DOCENTE",
                "usuarioId", 5L,
                "docenteId", 5L);
        mockMvc.perform(post("/api/participaciones/iniciar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(claims, Instant.now().plusSeconds(600)))
                        .header("X-Rol", "ESTUDIANTE")
                        .header("X-Usuario-Id", "10")
                        .header("X-Estudiante-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenSinRolEs403() throws Exception {
        mockMvc.perform(get("/api/participaciones")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Map.of("usuarioId", 10L), Instant.now().plusSeconds(600))))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> claimsEstudiante() {
        return Map.of("rol", "ESTUDIANTE", "usuarioId", 10L, "estudianteId", 10L);
    }

    private String bearer(Map<String, Object> claims, Instant expira) {
        Instant emitido = expira.minusSeconds(120);
        if (emitido.isAfter(Instant.now())) {
            emitido = Instant.now().minusSeconds(5);
        }
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .subject("10")
                .issuedAt(emitido)
                .expiresAt(expira);
        claims.forEach(builder::claim);
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                builder.build())).getTokenValue();
        return "Bearer " + token;
    }
}
