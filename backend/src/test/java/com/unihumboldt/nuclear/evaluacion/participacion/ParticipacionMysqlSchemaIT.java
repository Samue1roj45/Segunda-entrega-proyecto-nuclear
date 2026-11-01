package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
