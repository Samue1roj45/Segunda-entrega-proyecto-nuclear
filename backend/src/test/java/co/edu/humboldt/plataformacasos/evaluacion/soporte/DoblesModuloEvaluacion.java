package co.edu.humboldt.plataformacasos.evaluacion.soporte;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActualResolver;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PoliticaInicioIntento;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

/**
 * Pruebas de los módulos 5 y 6 aisladas de los demás: reemplaza los puertos del paquete contrato por
 * dobles en memoria. Se importa con {@code @Import(DoblesModuloEvaluacion.class)}. La integración real
 * de los puertos se prueba en aplicacion/ (FlujoCompletoTest).
 */
@TestConfiguration
public class DoblesModuloEvaluacion {

    @Bean
    @Primary
    CatalogoAcademicoEnMemoria catalogoAcademicoEnMemoria() {
        return new CatalogoAcademicoEnMemoria();
    }

    @Bean
    @Primary
    ActorActualResolver actorDeClaimsJwt() {
        return new ActorDeClaimsJwt();
    }

    @Bean
    @Primary
    PoliticaInicioIntento politicaInicioEnMemoria(CatalogoAcademicoEnMemoria catalogo, Clock clock) {
        return new PoliticaInicioEnMemoria(catalogo, clock);
    }
}
