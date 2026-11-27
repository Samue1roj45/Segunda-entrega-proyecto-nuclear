package co.edu.humboldt.plataformacasos.configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Recuperación segura de una migración fallida (por ejemplo, en Render). MySQL no revierte los cambios de estructura,
 * así que una migración que falla deja su fila con success=0 en flyway_schema_history y bloquea los arranques
 * siguientes. Con {@code FLYWAY_REPARAR=true} SOMA ejecuta «repair» (solo borra las filas fallidas y realinea las
 * sumas de verificación; no toca datos ni tablas de la aplicación) y después migra con normalidad.
 * Debe usarse solo durante el despliegue de recuperación y quitarse después. Apagada por defecto.
 */
@Configuration
@ConditionalOnProperty(name = "app.flyway.reparar", havingValue = "true")
class ReparacionFlyway {

    private static final Logger log = LoggerFactory.getLogger(ReparacionFlyway.class);

    @Bean
    FlywayMigrationStrategy repararYMigrar() {
        return flyway -> {
            log.warn("FLYWAY_REPARAR=true: reparando el historial de Flyway antes de migrar.");
            flyway.repair();
            flyway.migrate();
        };
    }
}
