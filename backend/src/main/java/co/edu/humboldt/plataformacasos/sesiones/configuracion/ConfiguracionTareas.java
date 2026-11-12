package co.edu.humboldt.plataformacasos.sesiones.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 #REQUISITO RF-19 || RF-23
 Para qué sirve: activa las tareas programadas (@Scheduled): cierre automático de
 sesiones vencidas y expiración de intentos sin tiempo.
 */

@Configuration
@EnableScheduling
public class ConfiguracionTareas {
}
