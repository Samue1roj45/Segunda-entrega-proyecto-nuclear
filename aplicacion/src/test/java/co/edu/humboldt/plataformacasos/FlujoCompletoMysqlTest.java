package co.edu.humboldt.plataformacasos;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El mismo flujo de punta a punta sobre MySQL 8 real, con las migraciones de Flyway de los seis módulos
 * y validando que las entidades JPA coinciden con el esquema (ddl-auto=validate). Requiere una base vacía y
 * desechable en MYSQL_FLUJO_URL (usuario y clave en MYSQL_PRUEBAS_USUARIO y MYSQL_PRUEBAS_CLAVE).
 * Sin esa variable se omite.
 */
@EnabledIfEnvironmentVariable(named = "MYSQL_FLUJO_URL", matches = ".+")
class FlujoCompletoMysqlTest extends FlujoCompletoTest {

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry propiedades) {
        propiedades.add("spring.datasource.url", () -> System.getenv("MYSQL_FLUJO_URL"));
        propiedades.add("spring.datasource.username", () -> System.getenv("MYSQL_PRUEBAS_USUARIO"));
        propiedades.add("spring.datasource.password", () -> System.getenv("MYSQL_PRUEBAS_CLAVE"));
        propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
