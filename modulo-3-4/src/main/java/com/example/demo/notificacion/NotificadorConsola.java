package com.example.demo.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-20
 Patrón: STRATEGY (estrategia concreta: simulación)
 Para qué sirve: "envía" el correo escribiéndolo en el log. Permite probar el flujo
 completo sin servidor SMTP.
 */
@Component
public class NotificadorConsola implements NotificadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(NotificadorConsola.class);

    @Override
    public void enviar(CorreoPendiente c) {
        log.info("[CORREO SIMULADO] Para: {} | Asunto: {}{}{}",
                c.destinatario(), c.asunto(), System.lineSeparator(), c.contenido());
    }
}
