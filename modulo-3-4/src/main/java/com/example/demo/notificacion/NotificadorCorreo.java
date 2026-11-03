package com.example.demo.notificacion;
/*
 #REQUISITO RF-20
 Patrón: STRATEGY (interfaz)
 Para qué sirve: define CÓMO se entrega un correo.
 @Primary. Nada más del sistema cambia (Abierto/Cerrado, SOLID).
 */
public interface NotificadorCorreo {
    void enviar(CorreoPendiente correo);
}
