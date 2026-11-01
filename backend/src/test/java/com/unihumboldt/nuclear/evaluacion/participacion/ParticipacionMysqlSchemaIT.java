package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
