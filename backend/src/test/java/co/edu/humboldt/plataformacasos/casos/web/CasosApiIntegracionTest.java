package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.integracion.ClaveEvaluacionDTO;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoParticipacionDTO;
import co.edu.humboldt.plataformacasos.soporte.Sesiones;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración reales: JWT, Proxy, servicios, JPA, migraciones Flyway y base de datos.
 * No son transaccionales a propósito: cada petición confirma su propia transacción, de modo que lo
 * leído después viene de la base y no de la caché de Hibernate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CasosApiIntegracionTest {

    private static final String CASOS = "/api/casos";
    private static final String CARLOS = "carlos.gomez@institucion.edu.co";
    private static final String LAURA = "laura.mejia@institucion.edu.co";

    private static final String DOCUMENTO_VALIDO = """
            {"dsl":"DSL-CEPS","version":"1.1","caso":{
              "nombre":"Violencia familiar","desarrolloSituacional":"Desarrollo","contextoHistoria":"Historia",
              "tiempoEstimadoMinutos":30,
              "escenarios":[{"nombre":"Hospital","contexto":"Urgencia vital","preguntas":[
                {"enunciado":"¿En qué centrar la intervención?","respuestas":[
                  {"id":"A","texto":"Notificar de inmediato","esCorrecta":false,"porcentaje":0,"justificacion":"Debe darse con protocolo"},
                  {"id":"B","texto":"Contención emocional","esCorrecta":true,"porcentaje":100,"justificacion":"PAP estabiliza primero"}
                ]}]}],
              "rubrica":{"nombre":"Rúbrica","descripcion":"Criterios","criteriosEvaluacion":[
                {"criterio":"Toma de decisiones","parametros":["Pertinencia","Justificación"],"rango":{"min":0,"max":5}}
              ]},
              "herramientas":[]}}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ContenidoCasoInterno contenidoInterno;

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM casos_estudio");
        jdbc.update("DELETE FROM usuario_permiso WHERE permiso <> 'GESTIONAR_CASOS' OR usuario_id = " + idDe(LAURA));
    }

    private long idDe(String correo) {
        return jdbc.queryForObject("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, correo);
    }

    private void otorgar(String correo, String permiso) {
        jdbc.update("INSERT INTO usuario_permiso (usuario_id, permiso) VALUES (?, ?)", idDe(correo), permiso);
    }

    private String sesionPersonal(String correo, String clave) throws Exception {
        Sesiones sesiones = new Sesiones(mockMvc, json);
        return sesiones.token(sesiones.login(Sesiones.RUTA_LOGIN_PERSONAL, Sesiones.cuerpoPersonal(correo, clave)));
    }

    private String carlos() throws Exception {
        return sesionPersonal(CARLOS, "Clave-Docente-01");
    }

    private String laura() throws Exception {
        return sesionPersonal(LAURA, "Clave-Docente-02");
    }

    private String estudiante() throws Exception {
        Sesiones sesiones = new Sesiones(mockMvc, json);
        return sesiones.token(sesiones.login(Sesiones.RUTA_LOGIN_ESTUDIANTE,
                Sesiones.cuerpoEstudiante("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01")));
    }

    private record Respuesta(int estado, JsonNode cuerpo, String texto) {
    }

    private Respuesta enviar(HttpMethod metodo, String ruta, String token, String cuerpo) throws Exception {
        MockHttpServletRequestBuilder solicitud = MockMvcRequestBuilders.request(metodo, ruta);
        if (token != null) {
            solicitud.header("Authorization", "Bearer " + token);
        }
        if (cuerpo != null) {
            solicitud.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        MvcResult resultado = mockMvc.perform(solicitud).andReturn();
        String texto = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Respuesta(resultado.getResponse().getStatus(), texto.isEmpty() ? null : json.readTree(texto), texto);
    }

    private Respuesta get(String ruta, String token) throws Exception {
        return enviar(HttpMethod.GET, ruta, token, null);
    }

    private Respuesta post(String ruta, String token, String cuerpo) throws Exception {
        return enviar(HttpMethod.POST, ruta, token, cuerpo);
    }

    private Respuesta put(String ruta, String token, String cuerpo) throws Exception {
        return enviar(HttpMethod.PUT, ruta, token, cuerpo);
    }

    private Respuesta delete(String ruta, String token) throws Exception {
        return enviar(HttpMethod.DELETE, ruta, token, null);
    }

    private long crearCaso(String token, String cuerpo) throws Exception {
        Respuesta creado = post(CASOS, token, cuerpo);
        assertThat(creado.estado()).isEqualTo(201);
        return creado.cuerpo().get("id").asLong();
    }

    private long importarCasoValido(String token) throws Exception {
        Respuesta importado = post(CASOS + "/importacion-dsl", token, DOCUMENTO_VALIDO);
        assertThat(importado.estado()).isEqualTo(201);
        return importado.cuerpo().get("caso").get("id").asLong();
    }

    private long casoPublicado(String token) throws Exception {
        long id = importarCasoValido(token);
        assertThat(post(CASOS + "/" + id + "/publicacion", token, null).estado()).isEqualTo(200);
        return id;
    }

    private static List<String> reglas(JsonNode incumplimientos) {
        List<String> reglas = new ArrayList<>();
        incumplimientos.forEach(i -> reglas.add(i.get("regla").asText()));
        return reglas;
    }

    @Test
    @DisplayName("RF-05: un docente autorizado crea un borrador incompleto, lo consulta, lo edita y lo lista")
    void registroDelCaso() throws Exception {
        String token = carlos();

        Respuesta creado = post(CASOS, token, "{\"nombre\":\"Mi caso\"}");
        assertThat(creado.estado()).isEqualTo(201);
        assertThat(creado.cuerpo().get("estado").asText()).isEqualTo("BORRADOR");
        long id = creado.cuerpo().get("id").asLong();

        Respuesta editado = put(CASOS + "/" + id, token, """
                {"nombre":"Editado","desarrolloSituacional":"D","contextoHistoria":"H","tiempoEstimadoMinutos":20}""");
        assertThat(editado.estado()).isEqualTo(200);

        JsonNode consultado = get(CASOS + "/" + id, token).cuerpo();
        assertThat(consultado.get("nombre").asText()).isEqualTo("Editado");
        assertThat(consultado.get("tiempoEstimadoMinutos").asInt()).isEqualTo(20);
        assertThat(consultado.get("docenteId").asLong()).isEqualTo(idDe(CARLOS));

        JsonNode lista = get(CASOS, token).cuerpo();
        assertThat(lista).hasSize(1);
        assertThat(get(CASOS + "?estado=PUBLICADO", token).cuerpo()).isEmpty();
        assertThat(get(CASOS + "?estado=NO_EXISTE", token).estado()).isEqualTo(400);
    }

    @Test
    @DisplayName("RF-05 a RF-09 y RF-17: construye un caso paso a paso, corrige lo que falta y lo publica")
    void flujoCompletoDelDocente() throws Exception {
        String token = carlos();
        long id = crearCaso(token, "{}");
        String caso = CASOS + "/" + id;

        // Vacío: el informe enumera todo lo que falta y el caso no cambia de estado.
        Respuesta rechazo = post(caso + "/publicacion", token, null);
        assertThat(rechazo.estado()).isEqualTo(422);
        assertThat(reglas(rechazo.cuerpo().get("incumplimientos")))
                .containsExactly("OCL-01", "OCL-02", "OCL-03", "OCL-09");
        assertThat(get(caso, token).cuerpo().get("estado").asText()).isEqualTo("BORRADOR");

        // RF-05 y RF-06
        put(caso, token, "{\"nombre\":\"Caso\",\"tiempoEstimadoMinutos\":30}");
        long idEscenario = post(caso + "/escenarios", token, "{\"nombre\":\"Hospital\"}").cuerpo().get("id").asLong();
        assertThat(reglas(get(caso + "/validacion", token).cuerpo().get("incumplimientos")))
                .containsExactly("OCL-04", "OCL-04", "OCL-09");
        put(caso + "/escenarios/" + idEscenario, token, "{\"nombre\":\"Hospital\",\"contexto\":\"Urgencia\"}");
        long idPregunta = post(caso + "/escenarios/" + idEscenario + "/preguntas", token,
                "{\"enunciado\":\"¿Qué hacer?\"}").cuerpo().get("id").asLong();

        // RF-07: una sola opción incompleta.
        long idA = post(caso + "/preguntas/" + idPregunta + "/respuestas", token,
                "{\"codigo\":\"A\",\"texto\":\"Opción A\"}").cuerpo().get("id").asLong();
        assertThat(reglas(get(caso + "/validacion", token).cuerpo().get("incumplimientos")))
                .containsExactly("OCL-05", "OCL-06", "OCL-06", "OCL-07", "OCL-09", "OCL-11");

        long idB = post(caso + "/preguntas/" + idPregunta + "/respuestas", token,
                "{\"codigo\":\"B\",\"texto\":\"Opción B\",\"porcentaje\":0}").cuerpo().get("id").asLong();
        put(caso + "/respuestas/" + idA, token, "{\"codigo\":\"A\",\"texto\":\"Opción A\",\"porcentaje\":100}");

        JsonNode marcada = put(caso + "/preguntas/" + idPregunta + "/respuesta-correcta", token,
                "{\"idRespuesta\":" + idA + "}").cuerpo();
        assertThat(marcada.get("respuestas").get(0).get("esCorrecta").asBoolean()).isTrue();
        assertThat(marcada.get("respuestas").get(1).get("esCorrecta").asBoolean()).isFalse();

        // RF-08
        put(caso + "/respuestas/" + idA + "/justificacion", token, "{\"justificacion\":\"Porque sí\"}");
        put(caso + "/respuestas/" + idB + "/justificacion", token, "{\"justificacion\":\"Porque no\"}");
        JsonNode justificacion = get(caso + "/respuestas/" + idB + "/justificacion", token).cuerpo();
        assertThat(justificacion.get("justificacion").asText()).isEqualTo("Porque no");
        assertThat(reglas(get(caso + "/validacion", token).cuerpo().get("incumplimientos")))
                .containsExactly("OCL-09");

        // RF-17
        assertThat(get(caso + "/rubrica", token).estado()).isEqualTo(404);
        assertThat(put(caso + "/rubrica", token, "{\"nombre\":\"Rúbrica\"}").estado()).isEqualTo(200);
        assertThat(reglas(post(caso + "/publicacion", token, null).cuerpo().get("incumplimientos")))
                .containsExactly("OCL-10");
        long idCriterio = post(caso + "/rubrica/criterios", token, """
                {"criterio":"Decisiones","parametros":["Pertinencia"],"rango":{"min":0}}""").cuerpo().get("id").asLong();
        assertThat(reglas(get(caso + "/validacion", token).cuerpo().get("incumplimientos")))
                .containsExactly("OCL-10");
        put(caso + "/rubrica/criterios/" + idCriterio, token, """
                {"criterio":"Decisiones","parametros":["Pertinencia"],"rango":{"min":0,"max":5}}""");

        // RF-09
        JsonNode informe = get(caso + "/validacion", token).cuerpo();
        assertThat(informe.get("valido").asBoolean()).isTrue();
        Respuesta publicado = post(caso + "/publicacion", token, null);
        assertThat(publicado.estado()).isEqualTo(200);
        assertThat(publicado.cuerpo().get("estado").asText()).isEqualTo("PUBLICADO");

        // Lo publicado es lo que quedó en la base, con todas sus relaciones.
        JsonNode persistido = get(caso, token).cuerpo();
        JsonNode pregunta = persistido.get("escenarios").get(0).get("preguntas").get(0);
        assertThat(pregunta.get("respuestas")).hasSize(2);
        assertThat(pregunta.get("respuestas").get(0).get("justificacion").asText()).isEqualTo("Porque sí");
        assertThat(persistido.get("rubrica").get("criteriosEvaluacion").get(0).get("rango").get("max").asInt())
                .isEqualTo(5);
        assertThat(persistido.get("fechaPublicacion").isNull()).isFalse();
    }

    @Test
    @DisplayName("RF-06 y RF-07: eliminar en un borrador borra sus elementos dependientes")
    void eliminacionEnBorrador() throws Exception {
        String token = carlos();
        long id = importarCasoValido(token);
        String caso = CASOS + "/" + id;
        JsonNode escenario = get(caso, token).cuerpo().get("escenarios").get(0);
        JsonNode pregunta = escenario.get("preguntas").get(0);
        long idRespuesta = pregunta.get("respuestas").get(0).get("id").asLong();

        assertThat(delete(caso + "/respuestas/" + idRespuesta, token).estado()).isEqualTo(204);
        assertThat(get(caso + "/respuestas/" + idRespuesta, token).estado()).isEqualTo(404);
        assertThat(delete(caso + "/preguntas/" + pregunta.get("id").asLong(), token).estado()).isEqualTo(204);
        assertThat(delete(caso + "/escenarios/" + escenario.get("id").asLong(), token).estado()).isEqualTo(204);
        assertThat(delete(caso + "/rubrica", token).estado()).isEqualTo(204);

        JsonNode despues = get(caso, token).cuerpo();
        assertThat(despues.get("escenarios")).isEmpty();
        assertThat(despues.get("rubrica").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM criterios_evaluacion", Integer.class)).isZero();
    }

    @Test
    @DisplayName("RF-09: el contenido publicado no se modifica por ninguna ruta y responde 409")
    void contenidoPublicadoNoSeEdita() throws Exception {
        String token = carlos();
        long id = casoPublicado(token);
        String caso = CASOS + "/" + id;
        JsonNode detalle = get(caso, token).cuerpo();
        JsonNode escenario = detalle.get("escenarios").get(0);
        JsonNode pregunta = escenario.get("preguntas").get(0);
        long idRespuesta = pregunta.get("respuestas").get(0).get("id").asLong();
        long idCriterio = detalle.get("rubrica").get("criteriosEvaluacion").get(0).get("id").asLong();

        List<Respuesta> intentos = List.of(
                put(caso, token, "{\"nombre\":\"Cambio\"}"),
                post(caso + "/escenarios", token, "{}"),
                put(caso + "/escenarios/" + escenario.get("id").asLong(), token, "{}"),
                delete(caso + "/escenarios/" + escenario.get("id").asLong(), token),
                post(caso + "/escenarios/" + escenario.get("id").asLong() + "/preguntas", token, "{}"),
                put(caso + "/preguntas/" + pregunta.get("id").asLong(), token, "{}"),
                delete(caso + "/preguntas/" + pregunta.get("id").asLong(), token),
                post(caso + "/preguntas/" + pregunta.get("id").asLong() + "/respuestas", token, "{}"),
                put(caso + "/preguntas/" + pregunta.get("id").asLong() + "/respuesta-correcta", token,
                        "{\"idRespuesta\":" + idRespuesta + "}"),
                put(caso + "/respuestas/" + idRespuesta, token, "{}"),
                put(caso + "/respuestas/" + idRespuesta + "/justificacion", token, "{\"justificacion\":\"Cambio\"}"),
                delete(caso + "/respuestas/" + idRespuesta, token),
                put(caso + "/rubrica", token, "{\"nombre\":\"Cambio\"}"),
                delete(caso + "/rubrica", token),
                post(caso + "/rubrica/criterios", token, "{}"),
                put(caso + "/rubrica/criterios/" + idCriterio, token, "{}"),
                delete(caso + "/rubrica/criterios/" + idCriterio, token));

        assertThat(intentos).extracting(Respuesta::estado).containsOnly(409);
        assertThat(get(caso, token).cuerpo()).isEqualTo(detalle);
    }

    @Test
    @DisplayName("RF-09: archivar solo un caso publicado, sin perder datos y sin volver a publicarlo")
    void archivo() throws Exception {
        String token = carlos();
        long borrador = importarCasoValido(token);
        assertThat(post(CASOS + "/" + borrador + "/archivo", token, null).estado()).isEqualTo(409);

        long id = casoPublicado(token);
        assertThat(post(CASOS + "/" + id + "/publicacion", token, null).estado()).isEqualTo(409);

        Respuesta archivado = post(CASOS + "/" + id + "/archivo", token, null);
        assertThat(archivado.estado()).isEqualTo(200);
        assertThat(archivado.cuerpo().get("estado").asText()).isEqualTo("ARCHIVADO");
        assertThat(archivado.cuerpo().get("fechaArchivado").isNull()).isFalse();
        assertThat(archivado.cuerpo().get("escenarios")).hasSize(1);
        assertThat(post(CASOS + "/" + id + "/publicacion", token, null).estado()).isEqualTo(409);
        assertThat(post(CASOS + "/" + id + "/archivo", token, null).estado()).isEqualTo(409);
    }

    @Test
    @DisplayName("Los valores fuera de dominio responden 400 al guardar, aun en borrador")
    void valoresInvalidos() throws Exception {
        String token = carlos();
        long id = importarCasoValido(token);
        String caso = CASOS + "/" + id;
        JsonNode pregunta = get(caso, token).cuerpo().get("escenarios").get(0).get("preguntas").get(0);

        assertThat(post(caso + "/preguntas/" + pregunta.get("id").asLong() + "/respuestas", token,
                "{\"codigo\":\"C\",\"porcentaje\":150}").estado()).isEqualTo(400);
        assertThat(post(caso + "/preguntas/" + pregunta.get("id").asLong() + "/respuestas", token,
                "{\"codigo\":\"ABC\"}").estado()).isEqualTo(400);
        Respuesta rango = post(caso + "/rubrica/criterios", token, "{\"rango\":{\"min\":5,\"max\":5}}");
        assertThat(rango.estado()).isEqualTo(400);
        assertThat(rango.cuerpo().get("mensaje").asText()).contains("menor que el máximo");
        assertThat(post(caso + "/rubrica/criterios", token,
                "{\"criterio\":\"" + "x".repeat(151) + "\"}").estado()).isEqualTo(400);
        assertThat(post(CASOS, token, "{\"nombre\":\"" + "x".repeat(151) + "\"}").estado()).isEqualTo(400);
        assertThat(post(CASOS, token, "{\"tiempoEstimadoMinutos\":\"texto\"}").estado()).isEqualTo(400);
        assertThat(put(caso + "/preguntas/" + pregunta.get("id").asLong() + "/respuesta-correcta", token, "{}").estado())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("RF-02: sin sesión responde 401; estudiante, administrador y docente sin permiso responden 403")
    void permisos() throws Exception {
        long id = importarCasoValido(carlos());
        String caso = CASOS + "/" + id;

        assertThat(get(CASOS, null).estado()).isEqualTo(401);
        assertThat(get(caso, null).estado()).isEqualTo(401);
        assertThat(post(CASOS, "token-falso", "{}").estado()).isEqualTo(401);

        String admin = sesionPersonal("admin.plataforma@institucion.edu.co", "Clave-Admin-01");
        for (String token : List.of(estudiante(), admin, laura())) {
            assertThat(get(CASOS, token).estado()).isEqualTo(403);
            assertThat(get(caso, token).estado()).isEqualTo(403);
            assertThat(post(CASOS, token, "{}").estado()).isEqualTo(403);
            assertThat(put(caso + "/rubrica", token, "{}").estado()).isEqualTo(403);
            assertThat(post(caso + "/publicacion", token, null).estado()).isEqualTo(403);
        }
    }

    @Test
    @DisplayName("Pertenencia: otro docente con permiso no ve ni modifica un caso ajeno")
    void pertenenciaDelRecurso() throws Exception {
        long id = importarCasoValido(carlos());
        String caso = CASOS + "/" + id;
        otorgar(LAURA, "GESTIONAR_CASOS");
        String otraDocente = laura();

        assertThat(get(caso, otraDocente).estado()).isEqualTo(403);
        assertThat(get(caso + "/validacion", otraDocente).estado()).isEqualTo(403);
        assertThat(get(caso + "/dsl", otraDocente).estado()).isEqualTo(403);
        assertThat(put(caso, otraDocente, "{}").estado()).isEqualTo(403);
        assertThat(post(caso + "/publicacion", otraDocente, null).estado()).isEqualTo(403);
        assertThat(get(CASOS, otraDocente).cuerpo()).isEmpty();
        assertThat(get(CASOS + "/999999", otraDocente).estado()).isEqualTo(404);
    }

    @Test
    @DisplayName("Un elemento de otro caso del mismo docente responde 404 por esa ruta")
    void elementoDeOtroCaso() throws Exception {
        String token = carlos();
        long unCaso = importarCasoValido(token);
        long otroCaso = importarCasoValido(token);
        long escenarioAjeno = get(CASOS + "/" + unCaso, token).cuerpo().get("escenarios").get(0).get("id").asLong();

        assertThat(put(CASOS + "/" + otroCaso + "/escenarios/" + escenarioAjeno, token, "{}").estado()).isEqualTo(404);
        assertThat(delete(CASOS + "/" + otroCaso + "/escenarios/" + escenarioAjeno, token).estado()).isEqualTo(404);
    }

    @Test
    @DisplayName("DSL-CEPS: exporta la sintaxis de la Figura 3, sin el estado, y se puede volver a importar")
    void exportacionDsl() throws Exception {
        String token = carlos();
        long id = casoPublicado(token);

        Respuesta exportado = get(CASOS + "/" + id + "/dsl", token);

        assertThat(exportado.estado()).isEqualTo(200);
        assertThat(exportado.cuerpo().get("dsl").asText()).isEqualTo("DSL-CEPS");
        assertThat(exportado.cuerpo().get("version").asText()).isEqualTo("1.1");
        assertThat(exportado.texto()).doesNotContain("estado").doesNotContain("PUBLICADO");
        JsonNode respuesta = exportado.cuerpo().get("caso").get("escenarios").get(0).get("preguntas").get(0)
                .get("respuestas").get(0);
        assertThat(respuesta.get("id").asText()).isEqualTo("A");

        Respuesta copia = post(CASOS + "/importacion-dsl", token, exportado.texto());
        assertThat(copia.estado()).isEqualTo(201);
        assertThat(copia.cuerpo().get("caso").get("estado").asText()).isEqualTo("BORRADOR");
        assertThat(get(CASOS + "/" + copia.cuerpo().get("caso").get("id").asLong() + "/validacion", token)
                .cuerpo().get("valido").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("DSL-CEPS: rechaza otro formato y avisa de las herramientas que no incorpora")
    void importacionDsl() throws Exception {
        String token = carlos();

        assertThat(post(CASOS + "/importacion-dsl", token, "{\"dsl\":\"OTRO\",\"version\":\"1.1\",\"caso\":{}}").estado())
                .isEqualTo(400);
        assertThat(post(CASOS + "/importacion-dsl", token, "{\"dsl\":\"DSL-CEPS\",\"version\":\"1.1\"}").estado())
                .isEqualTo(400);

        Respuesta conHerramientas = post(CASOS + "/importacion-dsl", token,
                "{\"dsl\":\"DSL-CEPS\",\"version\":\"1.1\",\"caso\":{\"herramientas\":[{\"nombre\":\"Protocolo\"}]}}");
        assertThat(conHerramientas.estado()).isEqualTo(201);
        assertThat(conHerramientas.cuerpo().get("advertencias").get(0).asText()).contains("RF-28");
    }

    @Test
    @DisplayName("Transacción: si un dato del documento es inválido no se guarda ninguna parte del caso")
    void importacionAtomica() throws Exception {
        String token = carlos();
        Integer antes = jdbc.queryForObject("SELECT COUNT(*) FROM casos_estudio", Integer.class);
        String invalido = DOCUMENTO_VALIDO.replace("\"max\":5", "\"max\":-5");

        assertThat(post(CASOS + "/importacion-dsl", token, invalido).estado()).isEqualTo(400);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM casos_estudio", Integer.class)).isEqualTo(antes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM escenarios", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Integrante 3: solo los casos publicados están disponibles para programar")
    void contratoProgramacion() throws Exception {
        String token = carlos();
        long borrador = importarCasoValido(token);
        long publicado = casoPublicado(token);
        long archivado = casoPublicado(token);
        post(CASOS + "/" + archivado + "/archivo", token, null);
        String ruta = "/api/integracion/casos";

        assertThat(get(ruta + "/publicados", token).estado()).isEqualTo(403);
        otorgar(CARLOS, "PROGRAMAR_SESIONES");

        Respuesta lista = get(ruta + "/publicados", token);
        assertThat(lista.estado()).isEqualTo(200);
        assertThat(lista.cuerpo()).hasSize(1);
        assertThat(lista.cuerpo().get(0).get("id").asLong()).isEqualTo(publicado);
        assertThat(lista.cuerpo().get(0).get("tiempoEstimadoMinutos").asInt()).isEqualTo(30);
        assertThat(lista.cuerpo().get(0).get("programable").asBoolean()).isTrue();
        assertThat(lista.texto()).doesNotContain("esCorrecta").doesNotContain("justificacion");

        assertThat(get(ruta + "/" + borrador + "/programacion", token).estado()).isEqualTo(404);
        assertThat(get(ruta + "/" + archivado + "/programacion", token).cuerpo().get("programable").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Integrante 4: consulta la rúbrica de un caso publicado o archivado, nunca de un borrador")
    void contratoEvaluacion() throws Exception {
        String token = carlos();
        long borrador = importarCasoValido(token);
        long archivado = casoPublicado(token);
        post(CASOS + "/" + archivado + "/archivo", token, null);
        String ruta = "/api/integracion/casos/";

        assertThat(get(ruta + archivado + "/rubrica", token).estado()).isEqualTo(403);
        otorgar(CARLOS, "CONSULTAR_PARTICIPACIONES");

        Respuesta rubrica = get(ruta + archivado + "/rubrica", token);
        assertThat(rubrica.estado()).isEqualTo(200);
        assertThat(rubrica.cuerpo().get("criteriosEvaluacion").get(0).get("criterio").asText())
                .isEqualTo("Toma de decisiones");
        assertThat(get(ruta + borrador + "/rubrica", token).estado()).isEqualTo(404);
    }

    @Test
    @DisplayName("RF-14: un estudiante no obtiene la clave ni las justificaciones por ningún endpoint")
    void estudianteNoVeLaClave() throws Exception {
        long id = casoPublicado(carlos());
        String estudiante = estudiante();
        String caso = CASOS + "/" + id;
        JsonNode pregunta = get(caso, carlos()).cuerpo().get("escenarios").get(0).get("preguntas").get(0);
        long idRespuesta = pregunta.get("respuestas").get(0).get("id").asLong();

        for (String ruta : List.of(caso, caso + "/dsl", caso + "/validacion", caso + "/rubrica",
                caso + "/preguntas/" + pregunta.get("id").asLong(), caso + "/respuestas/" + idRespuesta,
                caso + "/respuestas/" + idRespuesta + "/justificacion",
                "/api/integracion/casos/publicados", "/api/integracion/casos/" + id + "/rubrica")) {
            Respuesta intento = get(ruta, estudiante);
            assertThat(intento.estado()).as(ruta).isEqualTo(403);
            // El error repite la ruta solicitada, así que se busca el contenido real y no el nombre del campo.
            assertThat(intento.texto()).as(ruta).doesNotContain("esCorrecta")
                    .doesNotContain("PAP estabiliza").doesNotContain("Debe darse con protocolo");
        }
    }

    @Test
    @DisplayName("Puerto interno: el contenido del estudiante no incluye clave; el de evaluación sí")
    void contenidoInterno() throws Exception {
        String token = carlos();
        long borrador = importarCasoValido(token);
        long id = casoPublicado(token);

        ContenidoParticipacionDTO visible = contenidoInterno.contenidoSinClaves(id);
        String serializado = json.writeValueAsString(visible);
        assertThat(visible.escenarios().get(0).preguntas().get(0).opciones()).hasSize(2);
        assertThat(serializado).doesNotContain("esCorrecta").doesNotContain("porcentaje")
                .doesNotContain("justificacion").doesNotContain("PAP estabiliza");

        ClaveEvaluacionDTO clave = contenidoInterno.claveDeEvaluacion(id);
        assertThat(clave.preguntas().get(0).opciones())
                .filteredOn(ClaveEvaluacionDTO.OpcionClave::esCorrecta).hasSize(1);
        assertThat(clave.rubrica().criteriosEvaluacion()).hasSize(1);

        assertThatThrownBy(() -> contenidoInterno.contenidoSinClaves(borrador))
                .hasMessageContaining("no ha sido publicado");
    }

    @Test
    @DisplayName("Base de datos: restricciones de dominio, unicidad e integridad referencial")
    void restriccionesDeBaseDeDatos() throws Exception {
        long id = importarCasoValido(carlos());
        Long pregunta = jdbc.queryForObject("SELECT id_pregunta FROM preguntas LIMIT 1", Long.class);
        Long rubrica = jdbc.queryForObject("SELECT id_rubrica FROM rubricas WHERE id_caso = ?", Long.class, id);

        // Se exige el rechazo de la base: Spring no clasifica igual un CHECK de MySQL (error 3819) y uno de H2.

        assertThatThrownBy(() -> jdbc.update("INSERT INTO respuestas (id_pregunta, porcentaje) VALUES (?, 101)", pregunta))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO criterios_evaluacion (id_rubrica, rango_min, rango_max) VALUES (?, 5, 5)", rubrica))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO rubricas (id_caso) VALUES (?)", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE casos_estudio SET estado = 'OTRO' WHERE id_caso = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO casos_estudio (id_docente, estado, fecha_creacion, fecha_actualizacion)
                VALUES (999999, 'BORRADOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"""))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO preguntas (id_escenario, orden) VALUES (999999, 1)"))
                .isInstanceOf(DataAccessException.class);

        // Composición: al borrar el caso desaparecen sus partes.
        jdbc.update("DELETE FROM casos_estudio WHERE id_caso = ?", id);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM respuestas", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM criterios_evaluacion", Integer.class)).isZero();
    }
}
