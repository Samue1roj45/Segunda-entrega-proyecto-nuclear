package com.example.demo.configuracion;

import com.example.demo.politica.PoliticaIngresoTardio;
import com.example.demo.politica.PoliticaMitadDelTiempo;
import com.example.demo.politica.PoliticaPorcentajeConfigurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 #REQUISITO RF-22
 Patrón: STRATEGY (selección)
 Para qué sirve: elige la estrategia con la propiedad soma.ingreso-tardio.politica
 ("mitad" por defecto, o "porcentaje"). Un solo lugar decide; nadie más usa `if`.
*/

@Configuration
public class PoliticaIngresoConfig {

    @Bean
    public PoliticaIngresoTardio politicaIngresoTardio(
            @Value("${soma.ingreso-tardio.politica:mitad}") String politica) {
        return switch (politica.trim().toLowerCase()) {
            case "mitad" -> new PoliticaMitadDelTiempo();
            case "porcentaje" -> new PoliticaPorcentajeConfigurable();
            default -> throw new IllegalArgumentException(
                    "soma.ingreso-tardio.politica debe ser 'mitad' o 'porcentaje'");
        };
    }
}
