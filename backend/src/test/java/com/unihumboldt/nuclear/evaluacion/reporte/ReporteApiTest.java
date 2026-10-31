package com.unihumboldt.nuclear.evaluacion.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
class ReporteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogoAcademicoEnMemoria catalogo;

    @BeforeEach
    void seed() {
        catalogo.limpiar();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new EstudianteRef(10L, "est1@uni.edu", "Luis Estudiante", 1L));
        catalogo.registrar(new EstudianteRef(11L, "est2@uni.edu", "Ana Estudiante", 2L));
        catalogo.registrar(new PracticaRef(
                1L, "Practica 1", 50L, 5L,
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
    void rf16PdfYExcelVacios() throws Exception {
        MvcResult pdf = mockMvc.perform(get("/api/reportes/evaluacion.pdf")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andReturn();
        byte[] pdfBytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(new String(pdfBytes).startsWith("%PDF"));
        assertTrue(new String(pdfBytes).contains("Sin registros"));

        MvcResult xlsx = mockMvc.perform(get("/api/reportes/evaluacion.xlsx")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andReturn();
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx.getResponse().getContentAsByteArray()))) {
            assertEquals(0, libro.getSheet("evaluacion").getLastRowNum());
        }
    }

    @Test
    void rf16EstudianteNoDescargaAjenoNiVeOtroCorreo() throws Exception {
        finalizarParcial(10, 100);

        mockMvc.perform(get("/api/reportes/evaluacion.xlsx")
                        .param("estudiante", "11")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isForbidden());

        byte[] xlsx = mockMvc.perform(get("/api/reportes/evaluacion.xlsx")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        List<String> correos = correosExcel(xlsx);
        assertTrue(correos.contains("est1@uni.edu"));
        assertFalse(correos.contains("est2@uni.edu"));
    }

    @Test
    void rf16DocentePropioIncluyeIncompletaYAusenciaYAjeno403() throws Exception {
        iniciarYResponderUna(10, 100);
        mockMvc.perform(post("/api/resultados/practica/1/evaluar-pendientes")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reportes/evaluacion.pdf")
                        .param("practica", "1")
                        .with(user("docente99"))
                        .headers(doc(99)))
                .andExpect(status().isForbidden());

        byte[] pdf = mockMvc.perform(get("/api/reportes/evaluacion.pdf")
                        .param("practica", "1")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        String pdfTxt = new String(pdf);
        assertTrue(pdfTxt.contains("INCOMPLETA"));
        assertTrue(pdfTxt.contains("AUSENCIA"));
        assertTrue(pdfTxt.contains("est1@uni.edu"));
        assertTrue(pdfTxt.contains("est2@uni.edu"));

        byte[] xlsx = mockMvc.perform(get("/api/reportes/evaluacion.xlsx")
                        .param("practica", "1")
                        .with(user("docente5"))
                        .headers(doc(5)))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        List<String> clases = clasificacionesExcel(xlsx);
        assertTrue(clases.contains("INCOMPLETA"));
        assertTrue(clases.contains("AUSENCIA"));
    }

    @Test
    void rf16ExcelSeparaIntentos() throws Exception {
        finalizarParcial(10, 100);
        catalogo.actualizarAutorizacion(new AutorizacionRef(100L, 1L, 10L, "ABC123", true));
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

        byte[] xlsx = mockMvc.perform(get("/api/reportes/evaluacion.xlsx")
                        .with(user("est1"))
                        .headers(est(10)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheet("evaluacion");
            assertEquals(2, hoja.getLastRowNum());
            List<Integer> intentos = new ArrayList<>();
            List<Double> notas = new ArrayList<>();
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row row = hoja.getRow(i);
                intentos.add((int) row.getCell(7).getNumericCellValue());
                notas.add(row.getCell(10).getNumericCellValue());
            }
            assertTrue(intentos.contains(1));
            assertTrue(intentos.contains(2));
            assertTrue(notas.contains(50.0));
            assertTrue(notas.contains(100.0));
        }
    }

    private void finalizarParcial(long estudianteId, long autorizacionId) throws Exception {
        long pid = iniciar(estudianteId, autorizacionId);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est" + estudianteId))
                .headers(est(estudianteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est" + estudianteId))
                .headers(est(estudianteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":2,\"respuestaId\":22}"));
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(user("est" + estudianteId))
                .headers(est(estudianteId)));
    }

    private void iniciarYResponderUna(long estudianteId, long autorizacionId) throws Exception {
        long pid = iniciar(estudianteId, autorizacionId);
        mockMvc.perform(post("/api/participaciones/{id}/respuestas", pid)
                .with(user("est" + estudianteId))
                .headers(est(estudianteId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preguntaId\":1,\"respuestaId\":11}"));
    }

    private long iniciar(long estudianteId, long autorizacionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(user("est" + estudianteId))
                        .headers(est(estudianteId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":" + autorizacionId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(',', idStart);
        return Long.parseLong(body.substring(idStart, idEnd).trim());
    }

    private List<String> correosExcel(byte[] xlsx) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheet("evaluacion");
            List<String> out = new ArrayList<>();
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                out.add(hoja.getRow(i).getCell(5).getStringCellValue());
            }
            return out;
        }
    }

    private List<String> clasificacionesExcel(byte[] xlsx) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = libro.getSheet("evaluacion");
            List<String> out = new ArrayList<>();
            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                out.add(hoja.getRow(i).getCell(9).getStringCellValue());
            }
            return out;
        }
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
