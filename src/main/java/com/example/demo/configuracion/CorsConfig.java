package com.example.demo.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: permite que el frontend (otro puerto, p. ej. React en 5173 o 3000) llame a la API.
 Sin esto el navegador bloquea las peticiones. Los orígenes se cambian con soma.cors.origenes.
 */

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] origenes;

    public CorsConfig(@Value("${soma.cors.origenes:http://localhost:3000,http://localhost:5173,http://localhost:4200}")
                      String[] origenes) {
        this.origenes = origenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenes)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
