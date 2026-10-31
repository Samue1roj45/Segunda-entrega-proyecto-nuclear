package com.unihumboldt.nuclear.evaluacion.resultado;

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
import org.springframework.http.HttpHeaders;
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
import com.unihumboldt.nuclear.evaluacion.contrato.PreguntaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RespuestaOpcionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RubricaRef;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResultadoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CatalogoAcademicoEnMemoria catalogo;

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
        catalogo.registrar(new PreguntaRef(1L, 50L, "Pregunta 1", 2));
        catalogo.registrar(new PreguntaRef(2L, 50L, "Pregunta 2", 2));
        catalogo.registrar(new RespuestaOpcionRef(11L, 1L, "Correcta 1", true));
        catalogo.registrar(new RespuestaOpcionRef(12L, 1L, "Incorrecta 1", false));
        catalogo.registrar(new RespuestaOpcionRef(21L, 2L, "Correcta 2", true));
        catalogo.registrar(new RespuestaOpcionRef(22L, 2L, "Incorrecta 2", false));
        catalogo.registrar(new RubricaRef(1L, 50L, 100));
    }

    @Test
    void rf13CalculaNotaConPesosYEscala100() throws Exception {
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resultados")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notaFinal").value(50.00))
                .andExpect(jsonPath("$[0].escalaMaxima").value(100))
                .andExpect(jsonPath("$[0].correctas").value(1))
                .andExpect(jsonPath("$[0].incorrectas").value(1))
                .andExpect(jsonPath("$[0].pesoObtenido").value(2))
                .andExpect(jsonPath("$[0].pesoTotal").value(4));
    }

    @Test
    void rf13UsaEscalaDistintaDe100() throws Exception {
        catalogo.registrar(new RubricaRef(1L, 50L, 50));
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));

        mockMvc.perform(get("/api/resultados")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].escalaMaxima").value(50))
                .andExpect(jsonPath("$[0].notaFinal").value(25.00));
    }

    @Test
    void rf14EstudianteConsultaSuResultado() throws Exception {
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));
        JsonNode lista = leer(getResultados(10));
        long rid = lista.get(0).get("id").asLong();

        mockMvc.perform(get("/api/resultados/{id}", rid)
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participacionId").value(pid))
                .andExpect(jsonPath("$.detallePreguntas", hasSize(2)));
    }

    @Test
    void rf14EstudianteNoVeResultadoAjeno() throws Exception {
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));
        long rid = leer(getResultados(10)).get(0).get("id").asLong();

        mockMvc.perform(get("/api/resultados/{id}", rid)
                        .with(user("est2"))
                        .headers(est(11)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rf24IncompletaParcialYAusenciaCero() throws Exception {
        long pid = iniciarYResponderUnaCorrecta();
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incompletas").value(1))
                .andExpect(jsonPath("$.ausencias").value(1));

        mockMvc.perform(get("/api/resultados?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$[0].estadoParticipacion").value("INCOMPLETA"))
                .andExpect(jsonPath("$[0].notaFinal").value(50.00));

        MvcResult ausencia = mockMvc.perform(get("/api/resultados?estudiante=11")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$[0].estadoParticipacion").value("NO_INICIADA"))
                .andExpect(jsonPath("$[0].notaFinal").value(0.00))
                .andReturn();
        long ridAusencia = objectMapper.readTree(ausencia.getResponse().getContentAsString()).get(0).get("id").asLong();
        mockMvc.perform(get("/api/resultados/{id}", ridAusencia)
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoParticipacion").value("NO_INICIADA"));

        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$.incompletas").value(0))
                .andExpect(jsonPath("$.ausencias").value(0));
    }

    @Test
    void rf13SinPreguntasNotaCero() throws Exception {
        catalogo.limpiar();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new EstudianteRef(10L, "est1@uni.edu", "Luis", 1L));
        catalogo.registrar(new PracticaRef(1L, "P", 50L, 5L, ahora.minusHours(1), ahora.plusHours(3), "EN_CURSO", ""));
        catalogo.registrar(new AutorizacionRef(100L, 1L, 10L, "ABC123", false));

        long pid = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));

        mockMvc.perform(get("/api/resultados")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(jsonPath("$[0].notaFinal").value(0.00))
                .andExpect(jsonPath("$[0].pesoTotal").value(0))
                .andExpect(jsonPath("$[0].escalaMaxima").value(100));
    }

    @Test
    void rf14DocenteNoListaPracticaAjena() throws Exception {
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));

        mockMvc.perform(get("/api/resultados")
                        .with(user("docente99"))
                        .headers(doc(99)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente99"))
                        .headers(doc(99)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rf13ConservaResultadosDeIntentosAnteriores() throws Exception {
        long id1 = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id1)
                .with(user("est1"))
                .headers(est(10)));
        long id2 = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                        .with(user("est1"))
                        .headers(est(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":1,\"respuestaId\":11}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                        .with(user("est1"))
                        .headers(est(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":2,\"respuestaId\":21}"));
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id2)
                .with(user("est1"))
                .headers(est(10)));

        mockMvc.perform(get("/api/resultados")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numeroIntento").value(2))
                .andExpect(jsonPath("$[0].notaFinal").value(100.00))
                .andExpect(jsonPath("$[1].numeroIntento").value(1))
                .andExpect(jsonPath("$[1].notaFinal").value(50.00));
    }

    @Test
    void noReescribeNotaAlFinalizarOEvaluarDeNuevo() throws Exception {
        long pid = iniciarYResponderParcial();
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est1"))
                .headers(est(10)));
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resultados")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(jsonPath("$[0].notaFinal").value(50.00))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void ausenciaCeroSeConservaSiHaySegundoIntento() throws Exception {
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk());
        long id2 = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":2,\"respuestaId\":21}"));
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id2)
                .with(user("est1"))
                .headers(est(10)));

        mockMvc.perform(get("/api/resultados")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.estudianteId==10 && @.numeroIntento==1)].notaFinal").value(org.hamcrest.Matchers.hasItem(0.0)))
                .andExpect(jsonPath("$[?(@.estudianteId==10 && @.numeroIntento==2)].notaFinal").value(org.hamcrest.Matchers.hasItem(100.0)));
    }

    @Test
    void docenteSeleccionaNotaOficialSinAlterarIntentos() throws Exception {
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk());
        long id2 = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", id2)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":2,\"respuestaId\":21}"));
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", id2)
                .with(user("est1"))
                .headers(est(10)));

        JsonNode lista = leer(mockMvc.perform(get("/api/resultados?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk())
                .andReturn());
        long idAusencia = 0;
        long idSegundo = 0;
        for (JsonNode n : lista) {
            if (n.get("numeroIntento").asInt() == 1) {
                idAusencia = n.get("participacionId").asLong();
            } else {
                idSegundo = n.get("participacionId").asLong();
            }
        }

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("est1"))
                        .headers(est(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":10,\"participacionId\":" + idSegundo + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("docente99"))
                        .headers(doc(99))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":10,\"participacionId\":" + idSegundo + "}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("docente5"))
                        .headers(doc(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":10,\"participacionId\":99999}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("docente5"))
                        .headers(doc(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":11,\"participacionId\":" + idAusencia + "}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("docente5"))
                        .headers(doc(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":10,\"participacionId\":" + idAusencia + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroIntento").value(1))
                .andExpect(jsonPath("$.notaOficial").value(0.00));

        mockMvc.perform(get("/api/resultados?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$[?(@.numeroIntento==1)].oficial").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.numeroIntento==2)].oficial").value(org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$[?(@.numeroIntento==1)].notaFinal").value(org.hamcrest.Matchers.hasItem(0.0)))
                .andExpect(jsonPath("$[?(@.numeroIntento==2)].notaFinal").value(org.hamcrest.Matchers.hasItem(100.0)))
                .andExpect(jsonPath("$[?(@.numeroIntento==2)].notaOficial").value(org.hamcrest.Matchers.hasItem(0.0)));

        mockMvc.perform(post("/api/resultados/nota-oficial")
                        .with(user("docente5"))
                        .headers(doc(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"practicaId\":1,\"estudianteId\":10,\"participacionId\":" + idSegundo + "}"))
                .andExpect(jsonPath("$.numeroIntento").value(2))
                .andExpect(jsonPath("$.notaOficial").value(100.00));

        mockMvc.perform(get("/api/resultados?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$[?(@.numeroIntento==2)].oficial").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$[?(@.numeroIntento==1)].oficial").value(org.hamcrest.Matchers.hasItem(false)))
                .andExpect(jsonPath("$[?(@.numeroIntento==1)].notaFinal").value(org.hamcrest.Matchers.hasItem(0.0)))
                .andExpect(jsonPath("$[?(@.numeroIntento==2)].notaFinal").value(org.hamcrest.Matchers.hasItem(100.0)));

        mockMvc.perform(get("/api/resultados/notas-oficiales?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].participacionId").value(idSegundo));

        byte[] xlsx = mockMvc.perform(get("/api/reportes/evaluacion.xlsx?estudiante=10")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook libro =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            org.apache.poi.ss.usermodel.Sheet hoja = libro.getSheet("evaluacion");
            boolean hayOficial = false;
            boolean hayNoOficial = false;
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                String marca = hoja.getRow(i).getCell(19).getStringCellValue();
                if ("SI".equals(marca)) {
                    hayOficial = true;
                    org.junit.jupiter.api.Assertions.assertEquals(100.0, hoja.getRow(i).getCell(20).getNumericCellValue());
                } else {
                    hayNoOficial = true;
                }
            }
            org.junit.jupiter.api.Assertions.assertTrue(hayOficial);
            org.junit.jupiter.api.Assertions.assertTrue(hayNoOficial);
        }
    }

    @Test
    void responderPreguntaInvalidaEs400() throws Exception {
        long pid = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                        .with(user("est1"))
                        .headers(est(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preguntaId\":999,\"respuestaId\":11}"))
                .andExpect(status().isBadRequest());
    }

    private long iniciarYResponderParcial() throws Exception {
        long pid = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":2,\"respuestaId\":22}"));
        return pid;
    }

    private long iniciarYResponderUnaCorrecta() throws Exception {
        long pid = iniciar(10, 100);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est1"))
                .headers(est(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
        return pid;
    }

    private long iniciar(long estudianteId, long autorizacionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est" + estudianteId))
                        .headers(est(estudianteId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":" + autorizacionId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult getResultados(long estudianteId) throws Exception {
        return mockMvc.perform(get("/api/resultados")
                        .with(user("est" + estudianteId))
                        .headers(est(estudianteId)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode leer(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private HttpHeaders est(long id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-Rol", "ESTUDIANTE");
        h.add("X-Usuario-Id", String.valueOf(id));
        h.add("X-Estudiante-Id", String.valueOf(id));
        return h;
    }

    private HttpHeaders doc(long id) {
        HttpHeaders h = new HttpHeaders();
        h.add("X-Rol", "DOCENTE");
        h.add("X-Usuario-Id", String.valueOf(id));
        h.add("X-Docente-Id", String.valueOf(id));
        return h;
    }
}
