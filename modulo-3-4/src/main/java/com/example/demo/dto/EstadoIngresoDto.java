package com.example.demo.dto;

import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Para qué sirve: responde "¿puedo empezar mi participación ahora?": si está permitido,
 hasta cuándo se puede ingresar y, si no, el motivo (ingreso tardío, sesión sin iniciar...).
 */
public record EstadoIngresoDto(boolean permitido, String motivo, LocalDateTime limiteIngreso,
                               LocalDateTime ahora, String estadoSesion) {
}
