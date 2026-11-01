package com.example.demo.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 #REQUISITO RF-19 || RF-20 (y RF-23 más adelante)
 Para qué sirve: activa las tareas programadas (@Scheduled): cierre automático de
 sesiones y envío de notificaciones.
 */

@Configuration
@EnableScheduling
public class SchedulingConfig {
}
