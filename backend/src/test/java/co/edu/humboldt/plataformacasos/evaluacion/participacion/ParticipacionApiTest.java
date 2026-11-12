package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.evaluacion.soporte.DoblesModuloEvaluacion;
import org.springframework.context.annotation.Import;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import co.edu.humboldt.plataformacasos.evaluacion.soporte.ActorTokens;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.soporte.CatalogoAcademicoEnMemoria;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;

@SpringBootTest
@Import(DoblesModuloEvaluacion.class)
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
    private ParticipacionRepositorio repositorio;

    @BeforeEach
    void seed() {
        catalogo.limpiar();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new EstudianteRef(10L, "est1@uni.edu", "Luis Estudiante", 1L));
        catalogo.registrar(new EstudianteRef(11L, "est2@uni.edu", "Ana Estudiante", 2L));
        catalogo.registrar(new PracticaRef(
                1L, "Práctica 1", 50L, 5L,
                ahora.minusHours(1), ahora.plusHours(3), "EN_CURSO", "Bienvenidos", 240));
        catalogo.registrar(new AutorizacionRef(100L, 1L, 10L, true, 1));
        catalogo.registrar(new AutorizacionRef(101L, 1L, 11L, true, 1));
    }

    @Test
    void rf12IniciarYFinalizarGuardaTiempos() throws Exception {
        MvcResult inicio = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(ActorTokens.estudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.numeroIntento").value(1))
                .andExpect(jsonPath("$.inicio").isNotEmpty())
                .andReturn();

        long id = objectMapper.readTree(inicio.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id)
                        .with(ActorTokens.estudiante(10)))
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
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "No es posible finalizar una participación que no está en curso."));
    }

    @Test
    void rf12IntentosIndependientesConservanHistorial() throws Exception {
        JsonNode primero = iniciar(100, 10);
        long id1 = primero.get("id").asLong();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id1)
                .with(ActorTokens.estudiante(10)));

        catalogo.actualizarAutorizacion(new AutorizacionRef(100L, 1L, 10L, true, 2));

        JsonNode segundo = iniciar(100, 10);
        long id2 = segundo.get("id").asLong();

        org.junit.jupiter.api.Assertions.assertNotEquals(id1, id2);
        org.junit.jupiter.api.Assertions.assertEquals(2, segundo.get("numeroIntento").asInt());

        mockMvc.perform(get("/api/participaciones/{id}", id1)
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                .andExpect(jsonPath("$.numeroIntento").value(1));
    }

    @Test
    void rf22BloqueaInicioTrasLaMitadDelTiempoMaximo() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new PracticaRef(
                1L, "Práctica 1", 50L, 5L,
                ahora.minusHours(4), ahora.plusMinutes(10), "EN_CURSO", "Bienvenidos", 60));

        mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(ActorTokens.estudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Ingreso tardío")));
    }

    @Test
    void rf15DocenteListaSoloSusPracticas() throws Exception {
        iniciar(100, 10);

        mockMvc.perform(get("/api/participaciones")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/participaciones")
                        .with(ActorTokens.docente(99)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rf15EstudianteNoConsultaParticipacionAjena() throws Exception {
        JsonNode propio = iniciar(100, 10);
        long id = propio.get("id").asLong();

        mockMvc.perform(get("/api/participaciones/{id}", id)
                        .with(ActorTokens.estudiante(11)))
                .andExpect(status().isNotFound());
    }

    @Test
    void docenteNoFinalizaIntentoAjeno() throws Exception {
        JsonNode propio = iniciar(100, 10);
        long id = propio.get("id").asLong();

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id)
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void segundoIntentoTrasAusenciaNoReabreElPrimeroYElTerceroSeBloquea() throws Exception {
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk());

        MvcResult listado = mockMvc.perform(get("/api/participaciones?estudiante=10")
                        .with(ActorTokens.estudiante(10)))
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
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$.estado").value("NO_INICIADA"));

        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id2)
                .with(ActorTokens.estudiante(10)));

        mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(ActorTokens.estudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Ya usaste tu intento")));
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
                        .with(ActorTokens.estudiante(estudianteId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":" + autorizacionId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
