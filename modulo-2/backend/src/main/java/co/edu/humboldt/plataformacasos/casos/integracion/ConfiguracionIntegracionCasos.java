package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso;
import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

// Patrón Proxy de protección: cada bean expone el servicio real envuelto en ProxyAcceso (M1), que verifica el permiso @RequierePermiso antes de delegar.
/** Publica los contratos consultables por HTTP envueltos en el Proxy de RF-02. */
@Configuration
public class ConfiguracionIntegracionCasos {

    @Bean
    @Primary
    ConsultaCasosProgramables consultaCasosProgramables(ServicioIntegracionCasos servicioReal,
                                                        ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(ConsultaCasosProgramables.class, servicioReal, autorizacion);
    }

    @Bean
    @Primary
    ConsultaRubricasEvaluacion consultaRubricasEvaluacion(ServicioIntegracionCasos servicioReal,
                                                          ServicioAutorizacion autorizacion) {
        return ProxyAcceso.proteger(ConsultaRubricasEvaluacion.class, servicioReal, autorizacion);
    }
}
