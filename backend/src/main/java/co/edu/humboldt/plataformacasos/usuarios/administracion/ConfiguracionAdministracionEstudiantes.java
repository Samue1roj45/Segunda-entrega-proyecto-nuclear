package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso;
import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Patrón Proxy de protección: igual que ConfiguracionAdministracionDocentes, el controlador recibe el servicio envuelto en ProxyAcceso (M1).
/** Publica el registro de estudiantes envuelto en el Proxy de acceso de RF-02. */
@Configuration
public class ConfiguracionAdministracionEstudiantes {

    @Bean
    @Primary
    ServicioAdministracionEstudiantes servicioAdministracionEstudiantes(ServicioAdministracionEstudiantesImpl servicioReal,
                                                                        ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(ServicioAdministracionEstudiantes.class, servicioReal, autorizacion);
    }
}
