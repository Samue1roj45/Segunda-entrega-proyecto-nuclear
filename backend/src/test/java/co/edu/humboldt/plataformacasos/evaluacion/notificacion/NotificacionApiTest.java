package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import co.edu.humboldt.plataformacasos.evaluacion.soporte.DoblesModuloEvaluacion;
import org.springframework.context.annotation.Import;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.soporte.CatalogoAcademicoEnMemoria;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.soporte.ActorTokens;

@SpringBootTest
@Import(DoblesModuloEvaluacion.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificacionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogoAcademicoEnMemoria catalogo;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void seed() {
        catalogo.limpiar();
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        catalogo.registrar(new EstudianteRef(10L, "est1@uni.edu", "Luis Estudiante", 1L));
        catalogo.registrar(new EstudianteRef(11L, "est2@uni.edu", "Ana Estudiante", 2L));
        catalogo.registrar(new PracticaRef(
                1L, "Practica 1", 50L, 5L,
                ahora.minusHours(1), ahora.plusHours(3), "EN_CURSO", "Bienvenidos", 240));
        catalogo.registrar(new AutorizacionRef(100L, 1L, 10L, true, 1));
        catalogo.registrar(new AutorizacionRef(101L, 1L, 11L, true, 1));
    }

    @Test
    void rf21ConvocatoriaYCambioConPermisosYSinDuplicado() throws Exception {
        mockMvc.perform(post("/api/notificaciones/practica/1/convocatoria")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notificaciones/practica/1/convocatoria")
                        .with(ActorTokens.docente(99)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/notificaciones/practica/1/convocatoria")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviadas").value(2))
                .andExpect(jsonPath("$.omitidasDuplicado").value(0));

        mockMvc.perform(post("/api/notificaciones/practica/1/convocatoria")
                        .with(ActorTokens.docente(5)))
                .andExpect(jsonPath("$.enviadas").value(0))
                .andExpect(jsonPath("$.omitidasDuplicado").value(2));

        mockMvc.perform(post("/api/notificaciones/practica/1/cambio")
                        .with(ActorTokens.docente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"FECHAS\"}"))
                .andExpect(jsonPath("$.enviadas").value(2));
        mockMvc.perform(post("/api/notificaciones/practica/1/cambio")
                        .with(ActorTokens.docente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"FECHAS\"}"))
                .andExpect(jsonPath("$.omitidasDuplicado").value(2));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estudianteId").value(10));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(11)))
                .andExpect(jsonPath("$[0].estudianteId").value(11));

        verify(mailSender, times(4)).send(any(SimpleMailMessage.class));
    }

    @Test
    void rf26Y27RetroYCalificacionPublicada() throws Exception {
        long pid = iniciarYFinalizar();

        mockMvc.perform(post("/api/retroalimentaciones")
                        .with(ActorTokens.estudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participacionId\":" + pid + ",\"texto\":\"Bien\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/retroalimentaciones")
                        .with(ActorTokens.docente(99))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participacionId\":" + pid + ",\"texto\":\"Bien\"}"))
                .andExpect(status().isForbidden());

        MvcResult creado = mockMvc.perform(post("/api/retroalimentaciones")
                        .with(ActorTokens.docente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participacionId\":" + pid + ",\"texto\":\"Revisa el isotopo.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participacionId").value(pid))
                .andExpect(jsonPath("$.numeroIntento").value(1))
                .andExpect(jsonPath("$.estudianteId").value(10))
                .andExpect(jsonPath("$.practicaId").value(1))
                .andReturn();
        long rid = idDe(creado);

        mockMvc.perform(post("/api/retroalimentaciones")
                        .with(ActorTokens.docente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participacionId\":" + pid + ",\"texto\":\"Ajuste menor.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Ajuste menor."));

        mockMvc.perform(get("/api/retroalimentaciones/{id}", rid)
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Ajuste menor."));

        mockMvc.perform(get("/api/retroalimentaciones/{id}", rid)
                        .with(ActorTokens.estudiante(11)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/retroalimentaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estudianteId").value(10));

        mockMvc.perform(get("/api/retroalimentaciones")
                        .with(ActorTokens.estudiante(11)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/resultados/practica/1/publicar-calificaciones")
                        .with(ActorTokens.docente(99)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/resultados/practica/1/publicar-calificaciones")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviadas").value(1));
        mockMvc.perform(post("/api/resultados/practica/1/publicar-calificaciones")
                        .with(ActorTokens.docente(5)))
                .andExpect(jsonPath("$.omitidasDuplicado").value(1));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tipo", hasItem("CALIFICACION")))
                .andExpect(jsonPath("$[*].tipo", hasItem("RETROALIMENTACION")))
                .andExpect(jsonPath("$[?(@.tipo=='CALIFICACION')].estado", hasItem("ENVIADA")));
    }

    @Test
    void rf27ErrorSmtpNoModificaNiBorraResultado() throws Exception {
        long pid = iniciarYFinalizar();
        mockMvc.perform(get("/api/resultados")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participacionId").value(pid))
                .andExpect(jsonPath("$[0].notaFinal").value(0.00));

        doThrow(new MailSendException("smtp caido")).when(mailSender).send(any(SimpleMailMessage.class));

        mockMvc.perform(post("/api/resultados/practica/1/publicar-calificaciones")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errores").value(1));

        mockMvc.perform(get("/api/resultados")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].notaFinal").value(0.00));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$[0].tipo").value("CALIFICACION"))
                .andExpect(jsonPath("$[0].estado").value("ERROR"));

        reset(mailSender);

        mockMvc.perform(post("/api/resultados/practica/1/publicar-calificaciones")
                        .with(ActorTokens.docente(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviadas").value(1))
                .andExpect(jsonPath("$.omitidasDuplicado").value(0));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$[0].tipo").value("CALIFICACION"))
                .andExpect(jsonPath("$[0].estado").value("ENVIADA"));
    }

    @Test
    void rf27ErrorSmtpNoBorraRetroalimentacion() throws Exception {
        long pid = iniciarYFinalizar();
        doThrow(new MailSendException("smtp caido")).when(mailSender).send(any(SimpleMailMessage.class));

        mockMvc.perform(post("/api/retroalimentaciones")
                        .with(ActorTokens.docente(5))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participacionId\":" + pid + ",\"texto\":\"Comentario persistente.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.texto").value("Comentario persistente."));

        mockMvc.perform(get("/api/retroalimentaciones?participacion=" + pid)
                        .with(ActorTokens.docente(5)))
                .andExpect(jsonPath("$[0].texto").value("Comentario persistente."));

        mockMvc.perform(get("/api/notificaciones")
                        .with(ActorTokens.estudiante(10)))
                .andExpect(jsonPath("$[0].estado").value("ERROR"))
                .andExpect(jsonPath("$[0].errorMensaje").value("smtp caido"));
    }

    private long iniciarYFinalizar() throws Exception {
        MvcResult inicio = mockMvc.perform(post("/api/participaciones/iniciar")
                        .with(ActorTokens.estudiante(10))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizacionId\":100}"))
                .andExpect(status().isOk())
                .andReturn();
        long pid = idDe(inicio);
        mockMvc.perform(post("/api/participaciones/{id}/finalizar", pid)
                .with(ActorTokens.estudiante(10)));
        return pid;
    }

    private long idDe(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":") + 5;
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        return Long.parseLong(body.substring(start, end));
    }
}
