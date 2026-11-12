package co.edu.humboldt.plataformacasos.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj único de la aplicación. Todos los módulos toman la hora de aquí (nunca de LocalDateTime.now()
 * suelto), así las fechas de las sesiones, los intentos y las notificaciones usan la misma zona horaria
 * y las pruebas pueden fijar la hora.
 */
@Configuration
public class ConfiguracionReloj {

    @Bean
    Clock clock(@Value("${app.zona-horaria:America/Bogota}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
