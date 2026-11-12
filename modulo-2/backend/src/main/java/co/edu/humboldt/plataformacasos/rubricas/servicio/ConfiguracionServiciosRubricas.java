package co.edu.humboldt.plataformacasos.rubricas.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso;
import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Patrón Proxy de protección: cada bean expone el servicio real envuelto en ProxyAcceso (M1), que verifica el permiso @RequierePermiso antes de delegar.
/** Publica el servicio de RF-17 envuelto en el Proxy de RF-02. */
@Configuration
public class ConfiguracionServiciosRubricas {

    @Bean
    @Primary
    GestionRubricas gestionRubricas(ServicioRubricas servicioReal, ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(GestionRubricas.class, servicioReal, autorizacion);
    }
}
