package co.edu.humboldt.plataformacasos.usuarios.web;

import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Administración de cuentas de punta a punta (JWT, Proxy de acceso, cadenas de validación, JPA, migraciones
 * y semilla ficticia): archivar, reactivar, eliminar cuando es seguro y bloquear cuando hay historial.
 */
@SpringBootTest(properties = "pruebas.contexto=administracion-cuentas")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministracionUsuariosIntegracionTest {

    private static final String USUARIOS = "/api/admin/usuarios";
    private static final String ADMIN = "admin.plataforma@institucion.edu.co";
    private static final String CARLOS = "carlos.gomez@institucion.edu.co";
    private static final String LAURA = "laura.mejia@institucion.edu.co";
    private static final String ANA = "ana.perez@institucion.edu.co";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private record Respuesta(int estado, JsonNode cuerpo) {
    }

    private MvcResult login(String correo, String clave) throws Exception {
        return new Sesiones(mockMvc, json).login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave));
    }

    private String token(String correo, String clave) throws Exception {
        return new Sesiones(mockMvc, json).token(login(correo, clave));
    }

    private String admin() throws Exception {
        return token(ADMIN, "Clave-Admin-01");
    }

    private Respuesta enviar(HttpMethod metodo, String ruta, String token) throws Exception {
        MockHttpServletRequestBuilder solicitud = MockMvcRequestBuilders.request(metodo, ruta);
        if (token != null) {
            solicitud.header("Authorization", "Bearer " + token);
        }
        MvcResult r = mockMvc.perform(solicitud).andReturn();
        String texto = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(r.getResponse().getStatus(), texto.isEmpty() ? null : json.readTree(texto));
    }

    private long id(String correo) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, correo);
    }

    private List<String> correos(Respuesta listado) {
        List<String> correos = new ArrayList<>();
        listado.cuerpo().forEach(u -> correos.add(u.get("correoInstitucional").asString()));
        return correos;
    }

    private long auditoria(String accion) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE accion = ?", Long.class, accion);
    }

    @Test
    @DisplayName("Archivar un docente: no puede ingresar, sale de activos, aparece en archivados, conserva sus casos y se puede reactivar")
    void archivarYReactivar() throws Exception {
        String admin = admin();
        long carlos = id(CARLOS);
        long casos = jdbc.queryForObject("SELECT COUNT(*) FROM casos_estudio WHERE id_docente = ?", Long.class, carlos);
        String tokenPrevio = token(CARLOS, "Clave-Docente-01");
        long archivadosAntes = auditoria("USUARIO_ARCHIVADO");
        long reactivadosAntes = auditoria("USUARIO_REACTIVADO");

        Respuesta archivado = enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/archivo", admin);
        assertThat(archivado.estado()).isEqualTo(200);
        assertThat(archivado.cuerpo().get("activo").asBoolean()).isFalse();

        assertThat(login(CARLOS, "Clave-Docente-01").getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar(HttpMethod.GET, "/api/casos", tokenPrevio).estado()).isEqualTo(401);
        assertThat(correos(enviar(HttpMethod.GET, USUARIOS + "?activos=true", admin))).doesNotContain(CARLOS);
        assertThat(correos(enviar(HttpMethod.GET, USUARIOS + "?activos=false&rol=DOCENTE", admin))).contains(CARLOS);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM casos_estudio WHERE id_docente = ?", Long.class, carlos)).isEqualTo(casos);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/archivo", admin).estado()).isEqualTo(409);

        Respuesta reactivado = enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/reactivacion", admin);
        assertThat(reactivado.estado()).isEqualTo(200);
        assertThat(reactivado.cuerpo().get("activo").asBoolean()).isTrue();
        assertThat(login(CARLOS, "Clave-Docente-01").getResponse().getStatus()).isEqualTo(200);
        assertThat(correos(enviar(HttpMethod.GET, USUARIOS, admin))).contains(CARLOS);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/reactivacion", admin).estado()).isEqualTo(409);

        assertThat(auditoria("USUARIO_ARCHIVADO")).isEqualTo(archivadosAntes + 1);
        assertThat(auditoria("USUARIO_REACTIVADO")).isEqualTo(reactivadosAntes + 1);
        var registro = jdbc.queryForMap("SELECT actor_correo, detalle FROM auditoria WHERE accion = 'USUARIO_ARCHIVADO' ORDER BY id DESC LIMIT 1");
        assertThat(registro.get("actor_correo")).isEqualTo(ADMIN);
        assertThat((String) registro.get("detalle")).contains("Administrador de la Plataforma").contains(CARLOS);
    }

    @Test
    @DisplayName("Eliminar: una cuenta sin historial se borra; una con historial académico se bloquea, se explica y queda intacta")
    void eliminacionSegura() throws Exception {
        String admin = admin();
        long eliminadosAntes = auditoria("USUARIO_ELIMINADO");
        long bloqueadosAntes = auditoria("USUARIO_ELIMINACION_BLOQUEADA");
        MvcResult alta = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/docentes")
                .header("Authorization", "Bearer " + admin).contentType("application/json")
                .content("{\"nombreCompleto\":\"Docente Temporal\",\"correoInstitucional\":\"docente.temporal@institucion.edu.co\"}")).andReturn();
        assertThat(alta.getResponse().getStatus()).isEqualTo(201);
        long temporal = id("docente.temporal@institucion.edu.co");

        Respuesta evaluacion = enviar(HttpMethod.GET, USUARIOS + "/" + temporal + "/eliminacion", admin);
        assertThat(evaluacion.estado()).isEqualTo(200);
        assertThat(evaluacion.cuerpo().get("permitida").asBoolean()).isTrue();
        assertThat(enviar(HttpMethod.DELETE, USUARIOS + "/" + temporal, admin).estado()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id = ?", Long.class, temporal)).isZero();
        assertThat(auditoria("USUARIO_ELIMINADO")).isEqualTo(eliminadosAntes + 1);
        assertThat(enviar(HttpMethod.DELETE, USUARIOS + "/" + temporal, admin).estado()).isEqualTo(404);

        // Ana tiene convocatorias en la sesión de la semilla: no se puede borrar.
        long ana = id(ANA);
        long convocatorias = jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones WHERE id_estudiante = ?", Long.class, ana);
        assertThat(convocatorias).isPositive();
        Respuesta bloqueo = enviar(HttpMethod.GET, USUARIOS + "/" + ana + "/eliminacion", admin);
        assertThat(bloqueo.cuerpo().get("permitida").asBoolean()).isFalse();
        assertThat(bloqueo.cuerpo().get("motivo").asString()).contains("Convocatorias");
        assertThat(bloqueo.cuerpo().get("registros")).isNotEmpty();

        Respuesta intento = enviar(HttpMethod.DELETE, USUARIOS + "/" + ana, admin);
        assertThat(intento.estado()).isEqualTo(409);
        assertThat(intento.cuerpo().get("mensaje").asString()).contains("archivada");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id = ?", Long.class, ana)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM autorizaciones WHERE id_estudiante = ?", Long.class, ana)).isEqualTo(convocatorias);
        assertThat(auditoria("USUARIO_ELIMINACION_BLOQUEADA")).isEqualTo(bloqueadosAntes + 1);

        // El docente con casos tampoco, aunque esté archivado.
        long carlos = id(CARLOS);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/archivo", admin).estado()).isEqualTo(200);
        assertThat(enviar(HttpMethod.DELETE, USUARIOS + "/" + carlos, admin).estado()).isEqualTo(409);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + carlos + "/reactivacion", admin).estado()).isEqualTo(200);
    }

    @Test
    @DisplayName("El historial en tablas sin llave foránea (participaciones, retroalimentaciones) también protege la cuenta")
    void historialSinLlaveForanea() throws Exception {
        String admin = admin();
        jdbc.update("INSERT INTO usuario (correo_institucional, nombre_completo, rol, activo, fecha_creacion) VALUES ('estudiante.historial@institucion.edu.co', 'Estudiante con historial', 'ESTUDIANTE', TRUE, CURRENT_TIMESTAMP)");
        long estudiante = id("estudiante.historial@institucion.edu.co");
        long laura = id(LAURA);
        // Filas de evaluación sin llave hacia usuario: la base no impediría borrar estas cuentas.
        jdbc.update("INSERT INTO participacion (practica_id, estudiante_id, autorizacion_id, numero_intento, tiempo_usado_seg, estado) VALUES (999, ?, 999, 1, 0, 'FINALIZADA')", estudiante);
        long participacion = jdbc.queryForObject("SELECT id FROM participacion WHERE estudiante_id = ?", Long.class, estudiante);
        jdbc.update("INSERT INTO retroalimentacion (participacion_id, estudiante_id, practica_id, docente_id, texto, fecha_creacion, fecha_actualizacion) VALUES (?, ?, 999, ?, 'Prueba', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                participacion, estudiante, laura);

        Respuesta alumno = enviar(HttpMethod.DELETE, USUARIOS + "/" + estudiante, admin);
        assertThat(alumno.estado()).isEqualTo(409);
        assertThat(alumno.cuerpo().get("mensaje").asString()).contains("Participaciones");
        Respuesta docente = enviar(HttpMethod.DELETE, USUARIOS + "/" + laura, admin);
        assertThat(docente.estado()).isEqualTo(409);
        assertThat(docente.cuerpo().get("mensaje").asString()).contains("Retroalimentaciones");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE id IN (?, ?)", Long.class, estudiante, laura)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participacion WHERE estudiante_id = ?", Long.class, estudiante)).isEqualTo(1);
    }

    @Test
    @DisplayName("Cuentas protegidas: nadie se archiva ni se elimina a sí mismo; con otro administrador activo sí se puede archivar a uno")
    void cuentasProtegidas() throws Exception {
        String admin = admin();
        long yo = id(ADMIN);
        Respuesta propia = enviar(HttpMethod.POST, USUARIOS + "/" + yo + "/archivo", admin);
        assertThat(propia.estado()).isEqualTo(409);
        assertThat(propia.cuerpo().get("mensaje").asString()).contains("propia cuenta");
        assertThat(enviar(HttpMethod.DELETE, USUARIOS + "/" + yo, admin).estado()).isEqualTo(409);

        jdbc.update("INSERT INTO usuario (correo_institucional, nombre_completo, rol, activo, fecha_creacion) VALUES ('admin.dos@institucion.edu.co', 'Segundo Administrador', 'ADMINISTRADOR', TRUE, CURRENT_TIMESTAMP)");
        long otro = id("admin.dos@institucion.edu.co");
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + otro + "/archivo", admin).estado()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, yo)).isTrue();
    }

    @Test
    @DisplayName("Permisos: sin sesión 401; un docente o un estudiante no administran cuentas aunque llamen directo a la API")
    void permisos() throws Exception {
        long laura = id(LAURA);
        String carlos = token(CARLOS, "Clave-Docente-01");
        assertThat(enviar(HttpMethod.GET, USUARIOS, null).estado()).isEqualTo(401);
        assertThat(enviar(HttpMethod.GET, USUARIOS, carlos).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + laura + "/archivo", carlos).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/" + laura + "/reactivacion", carlos).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.GET, USUARIOS + "/" + laura + "/eliminacion", carlos).estado()).isEqualTo(403);
        assertThat(enviar(HttpMethod.DELETE, USUARIOS + "/" + laura, carlos).estado()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE id = ?", Boolean.class, laura)).isTrue();
        assertThat(enviar(HttpMethod.GET, USUARIOS + "?rol=INVENTADO", admin()).estado()).isEqualTo(400);
        assertThat(enviar(HttpMethod.POST, USUARIOS + "/999999/archivo", admin()).estado()).isEqualTo(404);
    }

    @Test
    @DisplayName("Dos archivos simultáneos no pueden dejar el sistema sin un administrador activo")
    void archivosSimultaneosNoDejanSinAdministrador() throws Exception {
        String hash = jdbc.queryForObject("SELECT clave_hash FROM usuario WHERE correo_institucional = ?", String.class, ADMIN);
        jdbc.update("""
                INSERT INTO usuario (correo_institucional, nombre_completo, rol, activo, fecha_creacion, clave_hash)
                VALUES ('admin.concurrente@institucion.edu.co', 'Administrador Concurrente', 'ADMINISTRADOR', TRUE, CURRENT_TIMESTAMP, ?)
                """, hash);
        long principal = id(ADMIN);
        long concurrente = id("admin.concurrente@institucion.edu.co");
        String tokenPrincipal = token(ADMIN, "Clave-Admin-01");
        String tokenConcurrente = token("admin.concurrente@institucion.edu.co", "Clave-Admin-01");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<Respuesta> archivaAlConcurrente = pool.submit(() -> archivarAlUnisono(listos, inicio, tokenPrincipal, concurrente));
            Future<Respuesta> archivaAlPrincipal = pool.submit(() -> archivarAlUnisono(listos, inicio, tokenConcurrente, principal));
            assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
            inicio.countDown();
            Respuesta una = archivaAlConcurrente.get(30, TimeUnit.SECONDS);
            Respuesta otra = archivaAlPrincipal.get(30, TimeUnit.SECONDS);

            long activos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMINISTRADOR' AND activo = TRUE", Long.class);
            assertThat(activos).isEqualTo(1);
            assertThat(List.of(una.estado(), otra.estado())).containsExactlyInAnyOrder(200, 409);
            Respuesta rechazada = una.estado() == 409 ? una : otra;
            assertThat(rechazada.cuerpo().get("mensaje").asString()).contains("último administrador");
        } finally {
            pool.shutdownNow();
            jdbc.update("UPDATE usuario SET activo = TRUE WHERE id IN (?, ?)", principal, concurrente);
        }
    }

    /** Espera a que las dos peticiones estén listas y entonces archiva, cada una en su propio hilo. */
    private Respuesta archivarAlUnisono(CountDownLatch listos, CountDownLatch inicio, String token, long idUsuario) throws Exception {
        listos.countDown();
        if (!inicio.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("La otra operación no llegó a arrancar.");
        }
        return enviar(HttpMethod.POST, USUARIOS + "/" + idUsuario + "/archivo", token);
    }
}
