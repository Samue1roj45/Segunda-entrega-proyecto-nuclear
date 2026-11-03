package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso;
import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Publica el servicio de RF-03 envuelto en el Proxy de acceso de RF-02: es el que reciben los controladores. */
@Configuration
public class ConfiguracionAdministracionDocentes {

    @Bean
    @Primary
    ServicioAdministracionDocentes servicioAdministracionDocentes(ServicioAdministracionDocentesImpl servicioReal,
                                                                  ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(ServicioAdministracionDocentes.class, servicioReal, autorizacion);
    }
}
