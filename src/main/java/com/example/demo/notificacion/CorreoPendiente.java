package com.example.demo.notificacion;
/*
 #REQUISITO RF-20
 Para qué sirve: un correo en cola listo para enviarse.
 */
public record CorreoPendiente(Long idNotificacion, String destinatario, String asunto, String contenido) {
}
