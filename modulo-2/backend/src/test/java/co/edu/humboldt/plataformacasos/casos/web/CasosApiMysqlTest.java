package co.edu.humboldt.plataformacasos.casos.web;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Repite toda la suite de integración sobre MySQL real: H2 no reproduce por completo sus bloqueos,
 * restricciones CHECK y tipos. Requiere una base vacía y desechable, porque aplica las migraciones y
 * los datos ficticios de prueba. Sin MYSQL_PRUEBAS_URL se omite y aparece como omitida en el informe.
 */
@EnabledIfEnvironmentVariable(named = "MYSQL_PRUEBAS_URL", matches = ".+")
class CasosApiMysqlTest extends CasosApiIntegracionTest {

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", () -> System.getenv("MYSQL_PRUEBAS_URL"));
        propiedades.add("spring.datasource.username", () -> System.getenv("MYSQL_PRUEBAS_USUARIO"));
        propiedades.add("spring.datasource.password", () -> System.getenv("MYSQL_PRUEBAS_CLAVE"));
    }
}
