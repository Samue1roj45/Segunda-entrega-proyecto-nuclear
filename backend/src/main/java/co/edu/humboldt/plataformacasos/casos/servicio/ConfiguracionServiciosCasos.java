package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso;
import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Publica los servicios del módulo envueltos en el Proxy de RF-02: son los que reciben los controladores. */
@Configuration
public class ConfiguracionServiciosCasos {

    @Bean
    @Primary
    GestionCasos gestionCasos(ServicioCasos servicioReal, ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(GestionCasos.class, servicioReal, autorizacion);
    }

    @Bean
    @Primary
    GestionEscenarios gestionEscenarios(ServicioEscenarios servicioReal, ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(GestionEscenarios.class, servicioReal, autorizacion);
    }

    @Bean
    @Primary
    GestionPreguntas gestionPreguntas(ServicioPreguntas servicioReal, ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(GestionPreguntas.class, servicioReal, autorizacion);
    }
}
