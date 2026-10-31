package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unihumboldt.nuclear.evaluacion.contrato.AutorizacionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademicoEnMemoria;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ParticipacionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CatalogoAcademicoEnMemoria catalogo;

    @Autowired
    private ParticipacionRepository repositorio;

    @BeforeEach
    void seed() {
        catalogo.limpiar();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new EstudianteRef(10L, "est1@uni.edu", "Luis Estudiante", 1L));
        catalogo.registrar(new EstudianteRef(11L, "est2@uni.edu", "Ana Estudiante", 2L));
        catalogo.registrar(new PracticaRef(
                1L, "Práctica 1", 50L, 5L,
                ahora.minusHours(1), ahora.plusHours(3), "EN_CURSO", "Bienvenidos"));
        catalogo.registrar(new AutorizacionRef(100L, 1L, 10L, "ABC123", false));
        catalogo.registrar(new AutorizacionRef(101L, 1L, 11L, "XYZ789", false));
    }

    @Test
    void rf12IniciarYFinalizarGuardaTiempos() throws Exception {
        MvcResult inicio = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est1"))
                        .headers(headersEstudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.numeroIntento").value(1))
                .andExpect(jsonPath("$.inicio").isNotEmpty())
                .andReturn();

        long id = objectMapper.readTree(inicio.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id)
                        .with(user("est1"))
                        .headers(headersEstudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.fin").isNotEmpty())
                .andExpect(jsonPath("$.tiempoUsadoSeg").isNumber());
    }

    @Test
    void rf12NoFinalizaSiNuncaInicio() throws Exception {
        Participacion pendiente = new Participacion();
        pendiente.setPracticaId(1L);
        pendiente.setEstudianteId(10L);
        pendiente.setAutorizacionId(100L);
        pendiente.setNumeroIntento(1);
        pendiente.setEstado(EstadoParticipacion.NO_INICIADA);
        pendiente = repositorio.save(pendiente);

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pendiente.getId())
                        .with(user("est1"))
                        .headers(headersEstudiante(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "No es posible finalizar una participación que no está en curso."));
    }

    @Test
    void rf12IntentosIndependientesConservanHistorial() throws Exception {
        JsonNode primero = iniciar(100, 10);
        long id1 = primero.get("id").asLong();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id1)
                .with(user("est1"))
                .headers(headersEstudiante(10)));

        catalogo.actualizarAutorizacion(new AutorizacionRef(100L, 1L, 10L, "ABC123", true));

        JsonNode segundo = iniciar(100, 10);
        long id2 = segundo.get("id").asLong();

        org.junit.jupiter.api.Assertions.assertNotEquals(id1, id2);
        org.junit.jupiter.api.Assertions.assertEquals(2, segundo.get("numeroIntento").asInt());

        mockMvc.perform(get("/api/participaciones/{id}", id1)
                        .with(user("est1"))
                        .headers(headersEstudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.numeroIntento").value(1));
    }

    @Test
    void rn17BloqueaInicioTrasPuntoMedio() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new PracticaRef(
                1L, "Práctica 1", 50L, 5L,
                ahora.minusHours(4), ahora.plusMinutes(10), "EN_CURSO", "Bienvenidos"));

        mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est1"))
                        .headers(headersEstudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("mitad")));
    }

    @Test
    void rf15DocenteListaSoloSusPracticas() throws Exception {
        iniciar(100, 10);

        mockMvc.perform(get("/api/participaciones")
                        .with(user("docente5"))
                        .headers(headersDocente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/participaciones")
                        .with(user("docente99"))
                        .headers(headersDocente(99)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rf15EstudianteNoConsultaParticipacionAjena() throws Exception {
        JsonNode propio = iniciar(100, 10);
        long id = propio.get("id").asLong();

        mockMvc.perform(get("/api/participaciones/{id}", id)
                        .with(user("est2"))
                        .headers(headersEstudiante(11)))
                .andExpect(status().isNotFound());
    }

    @Test
    void docenteNoRespondeNiFinalizaIntentoAjeno() throws Exception {
        JsonNode propio = iniciar(100, 10);
        long id = propio.get("id").asLong();

        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id)
                        .with(user("docente5"))
                        .headers(headersDocente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":1,\"respuestaId\":11}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id)
                        .with(user("docente5"))
                        .headers(headersDocente(5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void segundoIntentoTrasAusenciaNoReabreElPrimeroYElTerceroSeBloquea() throws Exception {
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(headersDocente(5)))
                .andExpect(status().isOk());

        MvcResult listado = mockMvc.perform(get("/api/participaciones?estudiante=10")
                        .with(user("est1"))
                        .headers(headersEstudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("NO_INICIADA"))
                .andReturn();
        long idAusencia = objectMapper.readTree(listado.getResponse().getContentAsString()).get(0).get("id").asLong();

        JsonNode segundo = iniciar(100, 10);
        long id2 = segundo.get("id").asLong();
        org.junit.jupiter.api.Assertions.assertNotEquals(idAusencia, id2);
        org.junit.jupiter.api.Assertions.assertEquals(2, segundo.get("numeroIntento").asInt());

        mockMvc.perform(get("/api/participaciones/{id}", idAusencia)
                        .with(user("est1"))
                        .headers(headersEstudiante(10)))
                .andExpect(jsonPath("$.estado").value("NO_INICIADA"));

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id2)
                .with(user("est1"))
                .headers(headersEstudiante(10)));

        mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est1"))
                        .headers(headersEstudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("dos intentos")));
    }

    @Test
    void iniciarSinAutenticacionEs401() throws Exception {
        mockMvc.perform(post("/api/participaciones/iniciar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode iniciar(long autorizacionId, long estudianteId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est" + estudianteId))
                        .headers(headersEstudiante(estudianteId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":" + autorizacionId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.http.HttpHeaders headersEstudiante(long estudianteId) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Rol", "ESTUDIANTE");
        headers.add("X-Usuario-Id", String.valueOf(estudianteId));
        headers.add("X-Estudiante-Id", String.valueOf(estudianteId));
        return headers;
    }

    private org.springframework.http.HttpHeaders headersDocente(long docenteId) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("X-Rol", "DOCENTE");
        headers.add("X-Usuario-Id", String.valueOf(docenteId));
        headers.add("X-Docente-Id", String.valueOf(docenteId));
        return headers;
    }
}
