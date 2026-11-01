package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.unihumboldt.nuclear.evaluacion.notificacion.EstadoEnvio;
import com.unihumboldt.nuclear.evaluacion.notificacion.Notificacion;
import com.unihumboldt.nuclear.evaluacion.notificacion.NotificacionRepository;
import com.unihumboldt.nuclear.evaluacion.notificacion.Retroalimentacion;
import com.unihumboldt.nuclear.evaluacion.notificacion.RetroalimentacionRepository;
import com.unihumboldt.nuclear.evaluacion.notificacion.TipoNotificacion;
import com.unihumboldt.nuclear.evaluacion.resultado.NotaOficial;
import com.unihumboldt.nuclear.evaluacion.resultado.NotaOficialRepository;
import com.unihumboldt.nuclear.evaluacion.resultado.Resultado;
import com.unihumboldt.nuclear.evaluacion.resultado.ResultadoRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ParticipacionMysqlSchemaIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("evaluacion_it")
            .withUsername("it_user")
            .withPassword("it_pass")
            .withInitScript("db/mysql/V1__crear_tablas_evaluacion.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private ParticipacionRepository repositorio;

    @Autowired
    private ResultadoRepository resultados;

    @Autowired
    private NotaOficialRepository notasOficiales;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private RetroalimentacionRepository retroalimentaciones;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void rf12PersisteParticipacionYRechazaIntentoDuplicado() {
        Participacion guardada = repositorio.saveAndFlush(participacion(100L, 1));
        Long id = guardada.getId();
        assertNotNull(id);

        entityManager.clear();

        Participacion recargada = repositorio.findById(id).orElseThrow();
        assertEquals(id, recargada.getId());
        assertEquals(1L, recargada.getPracticaId());
        assertEquals(10L, recargada.getEstudianteId());
        assertEquals(100L, recargada.getAutorizacionId());
        assertEquals(1, recargada.getNumeroIntento());
        assertNull(recargada.getInicio());
        assertNull(recargada.getFin());
        assertEquals(0, recargada.getTiempoUsadoSeg());
        assertEquals(EstadoParticipacion.NO_INICIADA, recargada.getEstado());

        Participacion duplicada = participacion(100L, 1);
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.saveAndFlush(duplicada));
    }

    @Test
    void persisteResultadoYLoRecupera() {
        Participacion participacion = repositorio.saveAndFlush(participacion(200L, 1));
        OffsetDateTime fechaCalculo = OffsetDateTime.parse("2026-11-01T14:00:00-05:00");

        Resultado resultado = new Resultado();
        resultado.setParticipacion(participacion);
        resultado.setCorrectas(3);
        resultado.setIncorrectas(1);
        resultado.setNoRespondidas(0);
        resultado.setPesoObtenido(8);
        resultado.setPesoTotal(10);
        resultado.setEscalaMaxima(5);
        resultado.setNotaFinal(new BigDecimal("4.00"));
        resultado.setFechaCalculo(fechaCalculo);

        Resultado guardado = resultados.saveAndFlush(resultado);
        Long id = guardado.getId();
        assertNotNull(id);

        entityManager.clear();

        Resultado recargado = resultados.findById(id).orElseThrow();
        assertEquals(id, recargado.getId());
        assertEquals(participacion.getId(), recargado.getParticipacion().getId());
        assertEquals(3, recargado.getCorrectas());
        assertEquals(1, recargado.getIncorrectas());
        assertEquals(0, recargado.getNoRespondidas());
        assertEquals(8, recargado.getPesoObtenido());
        assertEquals(10, recargado.getPesoTotal());
        assertEquals(5, recargado.getEscalaMaxima());
        assertEquals(0, new BigDecimal("4.00").compareTo(recargado.getNotaFinal()));
        assertEquals(fechaCalculo.toInstant(), recargado.getFechaCalculo().toInstant());
    }

    @Test
    void persisteNotaOficialYLaRecupera() {
        Participacion participacion = repositorio.saveAndFlush(participacion(300L, 1));
        OffsetDateTime fechaSeleccion = OffsetDateTime.parse("2026-11-01T15:00:00-05:00");

        NotaOficial nota = new NotaOficial();
        nota.setPracticaId(participacion.getPracticaId());
        nota.setEstudianteId(participacion.getEstudianteId());
        nota.setParticipacion(participacion);
        nota.setDocenteId(50L);
        nota.setFechaSeleccion(fechaSeleccion);

        NotaOficial guardada = notasOficiales.saveAndFlush(nota);
        Long id = guardada.getId();
        assertNotNull(id);

        entityManager.clear();

        NotaOficial recargada = notasOficiales.findByPracticaIdAndEstudianteId(
                participacion.getPracticaId(), participacion.getEstudianteId()).orElseThrow();
        assertEquals(id, recargada.getId());
        assertEquals(1L, recargada.getPracticaId());
        assertEquals(10L, recargada.getEstudianteId());
        assertEquals(participacion.getId(), recargada.getParticipacion().getId());
        assertEquals(50L, recargada.getDocenteId());
        assertEquals(fechaSeleccion.toInstant(), recargada.getFechaSeleccion().toInstant());
    }

    @Test
    void rechazaNotaOficialDuplicadaPorPracticaYEstudiante() {
        Participacion intentoUno = repositorio.saveAndFlush(participacion(600L, 1));
        Participacion intentoDos = repositorio.saveAndFlush(participacion(601L, 2));

        NotaOficial primera = new NotaOficial();
        primera.setPracticaId(1L);
        primera.setEstudianteId(10L);
        primera.setParticipacion(intentoUno);
        primera.setDocenteId(50L);
        primera.setFechaSeleccion(OffsetDateTime.parse("2026-11-01T15:00:00-05:00"));
        assertNotNull(notasOficiales.saveAndFlush(primera).getId());

        NotaOficial duplicada = new NotaOficial();
        duplicada.setPracticaId(1L);
        duplicada.setEstudianteId(10L);
        duplicada.setParticipacion(intentoDos);
        duplicada.setDocenteId(51L);
        duplicada.setFechaSeleccion(OffsetDateTime.parse("2026-11-01T16:00:00-05:00"));

        DataIntegrityViolationException error = assertThrows(
                DataIntegrityViolationException.class,
                () -> notasOficiales.saveAndFlush(duplicada));
        assertTrue(
                mensajeDeCausas(error).contains("uk_nota_oficial_practica_estudiante"),
                () -> "Se esperaba el rechazo de uk_nota_oficial_practica_estudiante: "
                        + mensajeDeCausas(error));
    }

    @Test
    void persisteNotificacionYLaRecupera() {
        Participacion participacion = repositorio.saveAndFlush(participacion(400L, 1));
        OffsetDateTime fechaCreacion = OffsetDateTime.parse("2026-11-01T16:00:00-05:00");

        Notificacion notificacion = new Notificacion();
        notificacion.setTipo(TipoNotificacion.CALIFICACION);
        notificacion.setClaveIdempotencia("calificacion-practica-1-estudiante-10");
        notificacion.setPracticaId(participacion.getPracticaId());
        notificacion.setEstudianteId(participacion.getEstudianteId());
        notificacion.setParticipacionId(participacion.getId());
        notificacion.setDestinatarioCorreo("est1@uni.edu");
        notificacion.setAsunto("Calificacion publicada");
        notificacion.setCuerpo("Su nota oficial ya esta disponible.");
        notificacion.setEstado(EstadoEnvio.PENDIENTE);
        notificacion.setFechaCreacion(fechaCreacion);

        Notificacion guardada = notificaciones.saveAndFlush(notificacion);
        Long id = guardada.getId();
        assertNotNull(id);

        entityManager.clear();

        Notificacion recargada = notificaciones.findByClaveIdempotencia(
                "calificacion-practica-1-estudiante-10").orElseThrow();
        assertEquals(id, recargada.getId());
        assertEquals(TipoNotificacion.CALIFICACION, recargada.getTipo());
        assertEquals("calificacion-practica-1-estudiante-10", recargada.getClaveIdempotencia());
        assertEquals(1L, recargada.getPracticaId());
        assertEquals(10L, recargada.getEstudianteId());
        assertEquals(participacion.getId(), recargada.getParticipacionId());
        assertEquals("est1@uni.edu", recargada.getDestinatarioCorreo());
        assertEquals("Calificacion publicada", recargada.getAsunto());
        assertEquals("Su nota oficial ya esta disponible.", recargada.getCuerpo());
        assertEquals(EstadoEnvio.PENDIENTE, recargada.getEstado());
        assertNull(recargada.getErrorMensaje());
        assertEquals(fechaCreacion.toInstant(), recargada.getFechaCreacion().toInstant());
        assertNull(recargada.getFechaEnvio());
    }

    @Test
    void persisteRetroalimentacionYLaRecupera() {
        Participacion participacion = repositorio.saveAndFlush(participacion(500L, 1));
        OffsetDateTime fechaCreacion = OffsetDateTime.parse("2026-11-01T17:00:00-05:00");
        OffsetDateTime fechaActualizacion = OffsetDateTime.parse("2026-11-01T17:05:00-05:00");

        Retroalimentacion retroalimentacion = new Retroalimentacion();
        retroalimentacion.setParticipacion(participacion);
        retroalimentacion.setEstudianteId(participacion.getEstudianteId());
        retroalimentacion.setPracticaId(participacion.getPracticaId());
        retroalimentacion.setDocenteId(50L);
        retroalimentacion.setTexto("Revise las preguntas de ponderacion.");
        retroalimentacion.setFechaCreacion(fechaCreacion);
        retroalimentacion.setFechaActualizacion(fechaActualizacion);

        Retroalimentacion guardada = retroalimentaciones.saveAndFlush(retroalimentacion);
        Long id = guardada.getId();
        assertNotNull(id);

        entityManager.clear();

        Retroalimentacion recargada = retroalimentaciones.findByParticipacionId(participacion.getId())
                .orElseThrow();
        assertEquals(id, recargada.getId());
        assertEquals(participacion.getId(), recargada.getParticipacion().getId());
        assertEquals(10L, recargada.getEstudianteId());
        assertEquals(1L, recargada.getPracticaId());
        assertEquals(50L, recargada.getDocenteId());
        assertEquals("Revise las preguntas de ponderacion.", recargada.getTexto());
        assertEquals(fechaCreacion.toInstant(), recargada.getFechaCreacion().toInstant());
        assertEquals(fechaActualizacion.toInstant(), recargada.getFechaActualizacion().toInstant());
    }

    @Test
    void rechazaResultadoConParticipacionInexistente() {
        Resultado resultado = new Resultado();
        resultado.setParticipacion(entityManager.getEntityManager().getReference(Participacion.class, 9_999_999L));
        resultado.setCorrectas(0);
        resultado.setIncorrectas(0);
        resultado.setNoRespondidas(0);
        resultado.setPesoObtenido(0);
        resultado.setPesoTotal(10);
        resultado.setEscalaMaxima(5);
        resultado.setNotaFinal(new BigDecimal("0.00"));
        resultado.setFechaCalculo(OffsetDateTime.parse("2026-11-01T18:00:00-05:00"));

        DataIntegrityViolationException error = assertThrows(
                DataIntegrityViolationException.class,
                () -> resultados.saveAndFlush(resultado));
        assertTrue(
                mensajeDeCausas(error).contains("fk_resultado_participacion"),
                () -> "Se esperaba el rechazo de fk_resultado_participacion: " + mensajeDeCausas(error));
    }

    private static String mensajeDeCausas(Throwable error) {
        StringBuilder texto = new StringBuilder();
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual.getMessage() != null) {
                texto.append(actual.getMessage()).append(' ');
            }
        }
        return texto.toString().toLowerCase();
    }

    private static Participacion participacion(long autorizacionId, int numeroIntento) {
        Participacion participacion = new Participacion();
        participacion.setPracticaId(1L);
        participacion.setEstudianteId(10L);
        participacion.setAutorizacionId(autorizacionId);
        participacion.setNumeroIntento(numeroIntento);
        participacion.setTiempoUsadoSeg(0);
        participacion.setEstado(EstadoParticipacion.NO_INICIADA);
        return participacion;
    }
}
