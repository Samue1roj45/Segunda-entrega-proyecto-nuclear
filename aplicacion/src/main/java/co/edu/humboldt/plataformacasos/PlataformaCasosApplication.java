package co.edu.humboldt.plataformacasos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Backend único del Proyecto Nuclear: arranca los seis módulos en una sola aplicación (un solo contexto,
 * una sola base de datos y la seguridad del módulo 1). Cada módulo vive en su carpeta; ver el README.
 */
@SpringBootApplication
public class PlataformaCasosApplication {

    /** Zona horaria de la aplicación; se puede cambiar con la variable ZONA_HORARIA. */
    static final String ZONA_POR_DEFECTO = "America/Bogota";

    public static void main(String[] args) {
        String zona = System.getenv().getOrDefault("ZONA_HORARIA", ZONA_POR_DEFECTO);
        // Las fechas sin zona (LocalDateTime) de todos los módulos se interpretan en la misma zona que el reloj.
        TimeZone.setDefault(TimeZone.getTimeZone(zona));
        SpringApplication.run(PlataformaCasosApplication.class, args);
    }
}
