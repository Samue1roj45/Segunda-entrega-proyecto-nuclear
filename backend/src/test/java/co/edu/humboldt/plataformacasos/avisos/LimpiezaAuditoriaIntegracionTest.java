package co.edu.humboldt.plataformacasos.avisos;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Limpieza del historial de auditoría por fecha de corte, de punta a punta. */
@SpringBootTest(properties = "pruebas.contexto=limpieza-auditoria")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LimpiezaAuditoriaIntegracionTest {

    private static final String ADMIN = "admin.plataforma@institucion.edu.co";
    private static final String CARLOS = "carlos.gomez@institucion.edu.co";
    private static final LocalDate CORTE = LocalDate.of(2026, 11, 1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private String token(String correo, String clave) throws Exception {
        Sesiones s = new Sesiones(mockMvc, json);
        return s.token(s.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private void registroDel(LocalDateTime fecha, String accion) {
        jdbc.update("INSERT INTO auditoria (fecha, actor_id, actor_correo, accion, detalle) VALUES (?, NULL, 'prueba@institucion.edu.co', ?, 'Registro de prueba')",
                Timestamp.valueOf(fecha), accion);
    }

    private long contar(String condicion, Object... args) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE " + condicion, Long.class, args);
    }

    private MvcResult vistaPrevia(String token, String fecha) throws Exception {
        return mockMvc.perform(get("/api/auditoria/limpieza" + (fecha == null ? "" : "?antesDe=" + fecha))
                .header("Authorization", "Bearer " + token)).andReturn();
    }

    private MvcResult limpiar(String token, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/auditoria/limpieza").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
    }

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Vista previa, confirmación obligatoria, borrado solo de lo anterior al corte y registro de la propia limpieza")
    void limpiezaPorCorte() throws Exception {
        String admin = token(ADMIN, "Clave-Admin-01");
        Timestamp inicioCorte = Timestamp.valueOf(CORTE.atStartOfDay());
        registroDel(CORTE.minusDays(40).atTime(9, 0), "PRUEBA_ANTIGUA");
        registroDel(CORTE.minusDays(1).atTime(23, 59, 59), "PRUEBA_ANTIGUA");
        registroDel(CORTE.atStartOfDay(), "PRUEBA_RECIENTE");
        registroDel(CORTE.plusDays(3).atTime(8, 0), "PRUEBA_RECIENTE");
        long anteriores = contar("fecha < ?", inicioCorte);
        long posteriores = contar("fecha >= ? AND accion <> ?", inicioCorte, LimpiezaAuditoria.ACCION_LIMPIEZA);
        long limpiezasAntes = contar("accion = ?", LimpiezaAuditoria.ACCION_LIMPIEZA);
        assertThat(anteriores).isGreaterThanOrEqualTo(2);

        MvcResult previa = vistaPrevia(admin, CORTE.toString());
        assertThat(previa.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(previa).get("registros").asLong()).isEqualTo(anteriores);
        assertThat(contar("fecha < ?", inicioCorte)).isEqualTo(anteriores);

        // Sin confirmación explícita no se borra nada (es lo que pasa si el administrador cancela).
        assertThat(limpiar(admin, "{\"antesDe\":\"" + CORTE + "\"}").getResponse().getStatus()).isEqualTo(400);
        assertThat(limpiar(admin, "{\"antesDe\":\"" + CORTE + "\",\"confirmada\":false}").getResponse().getStatus()).isEqualTo(400);
        assertThat(contar("fecha < ?", inicioCorte)).isEqualTo(anteriores);

        MvcResult hecha = limpiar(admin, "{\"antesDe\":\"" + CORTE + "\",\"confirmada\":true}");
        assertThat(hecha.getResponse().getStatus()).isEqualTo(200);
        assertThat(cuerpo(hecha).get("eliminados").asLong()).isEqualTo(anteriores);
        assertThat(contar("fecha < ?", inicioCorte)).isZero();
        assertThat(contar("fecha >= ? AND accion <> ?", inicioCorte, LimpiezaAuditoria.ACCION_LIMPIEZA)).isEqualTo(posteriores);
        assertThat(contar("accion = 'PRUEBA_RECIENTE'")).isEqualTo(2);

        Map<String, Object> registro = jdbc.queryForMap("SELECT actor_correo, detalle, fecha FROM auditoria WHERE accion = ? ORDER BY id DESC LIMIT 1",
                LimpiezaAuditoria.ACCION_LIMPIEZA);
        assertThat(registro.get("actor_correo")).isEqualTo(ADMIN);
        assertThat((String) registro.get("detalle")).contains("Administrador de la Plataforma", String.valueOf(anteriores),
                "1 de noviembre de 2026", "correcto");

        // El registro de la limpieza sobrevive a una segunda limpieza con el mismo corte.
        assertThat(cuerpo(limpiar(admin, "{\"antesDe\":\"" + CORTE + "\",\"confirmada\":true}")).get("eliminados").asLong()).isZero();
        assertThat(contar("accion = ?", LimpiezaAuditoria.ACCION_LIMPIEZA)).isEqualTo(limpiezasAntes + 2);

        // La consulta, el filtro y la paginación siguen igual, y la auditoría sigue registrando.
        String filtrada = mockMvc.perform(get("/api/auditoria?accion=" + LimpiezaAuditoria.ACCION_LIMPIEZA + "&limite=1")
                .header("Authorization", "Bearer " + admin)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo(mockMvc.perform(get("/api/auditoria?accion=" + LimpiezaAuditoria.ACCION_LIMPIEZA + "&limite=1")
                .header("Authorization", "Bearer " + admin)).andReturn())).hasSize(1);
        assertThat(filtrada).contains(LimpiezaAuditoria.ACCION_LIMPIEZA);
        long ingresos = contar("accion = 'INGRESO'");
        token(ADMIN, "Clave-Admin-01");
        assertThat(contar("accion = 'INGRESO'")).isEqualTo(ingresos + 1);
    }

    @Test
    @DisplayName("La fecha es obligatoria y no puede ser futura; solo el administrador puede ver o ejecutar la limpieza")
    void validacionesYPermisos() throws Exception {
        String admin = token(ADMIN, "Clave-Admin-01");
        String carlos = token(CARLOS, "Clave-Docente-01");
        long total = contar("1 = 1");

        assertThat(vistaPrevia(admin, null).getResponse().getStatus()).isEqualTo(400);
        assertThat(vistaPrevia(admin, "2026-13-40").getResponse().getStatus()).isEqualTo(400);
        assertThat(vistaPrevia(admin, LocalDate.now().plusDays(2).toString()).getResponse().getStatus()).isEqualTo(400);
        assertThat(limpiar(admin, "{\"antesDe\":\"" + LocalDate.now().plusDays(2) + "\",\"confirmada\":true}").getResponse().getStatus()).isEqualTo(400);
        assertThat(limpiar(admin, "{\"confirmada\":true}").getResponse().getStatus()).isEqualTo(400);

        assertThat(vistaPrevia(carlos, CORTE.toString()).getResponse().getStatus()).isEqualTo(403);
        assertThat(limpiar(carlos, "{\"antesDe\":\"" + CORTE + "\",\"confirmada\":true}").getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(post("/api/auditoria/limpieza").contentType(MediaType.APPLICATION_JSON)
                .content("{\"antesDe\":\"" + CORTE + "\",\"confirmada\":true}")).andReturn().getResponse().getStatus()).isEqualTo(401);
        // Solo se sumaron los ingresos de esta prueba: nada se borró.
        assertThat(contar("1 = 1")).isGreaterThanOrEqualTo(total);
    }

    @Test
    @DisplayName("Una limpieza con corte de hoy borra lo anterior y conserva el registro de esa misma ejecución")
    void limpiezaDeHoyConservaSuPropioRegistro() throws Exception {
        String admin = token(ADMIN, "Clave-Admin-01");
        registroDel(LocalDate.now().minusDays(3).atTime(8, 0), "PRUEBA_HOY");
        registroDel(LocalDate.now().atTime(0, 30), "PRUEBA_DE_HOY");
        long limpiezasAntes = contar("accion = ?", LimpiezaAuditoria.ACCION_LIMPIEZA);

        MvcResult hoy = limpiar(admin, "{\"antesDe\":\"" + LocalDate.now() + "\",\"confirmada\":true}");
        assertThat(hoy.getResponse().getStatus()).isEqualTo(200);
        assertThat(contar("accion = 'PRUEBA_HOY'")).isZero();
        assertThat(contar("accion = 'PRUEBA_DE_HOY'")).isEqualTo(1);
        assertThat(contar("accion = ?", LimpiezaAuditoria.ACCION_LIMPIEZA)).isEqualTo(limpiezasAntes + 1);

        Map<String, Object> propia = jdbc.queryForMap(
                "SELECT actor_correo, detalle, fecha FROM auditoria WHERE accion = ? ORDER BY id DESC LIMIT 1",
                LimpiezaAuditoria.ACCION_LIMPIEZA);
        assertThat(propia.get("actor_correo")).isEqualTo(ADMIN);
        assertThat((String) propia.get("detalle")).contains("correcto");
        assertThat(((Timestamp) propia.get("fecha")).toLocalDateTime().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(cuerpo(hoy).get("eliminados").asLong()).isPositive();
    }
}
