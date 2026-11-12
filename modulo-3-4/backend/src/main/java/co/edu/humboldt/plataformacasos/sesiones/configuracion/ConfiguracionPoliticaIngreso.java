package co.edu.humboldt.plataformacasos.sesiones.configuracion;

import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaIngresoTardio;
import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaMitadDelTiempo;
import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaPorcentajeConfigurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 #REQUISITO RF-22
 Patrón: STRATEGY (selección)
 Para qué sirve: elige la estrategia con la propiedad app.sesiones.politica-ingreso-tardio
 ("mitad" por defecto, o "porcentaje"). Un solo lugar decide; nadie más usa `if`.
*/

@Configuration
public class ConfiguracionPoliticaIngreso {

    @Bean
    public PoliticaIngresoTardio politicaIngresoTardio(
            @Value("${app.sesiones.politica-ingreso-tardio:mitad}") String politica) {
        return switch (politica.trim().toLowerCase()) {
            case "mitad" -> new PoliticaMitadDelTiempo();
            case "porcentaje" -> new PoliticaPorcentajeConfigurable();
            default -> throw new IllegalArgumentException(
                    "app.sesiones.politica-ingreso-tardio debe ser 'mitad' o 'porcentaje'");
        };
    }
}
